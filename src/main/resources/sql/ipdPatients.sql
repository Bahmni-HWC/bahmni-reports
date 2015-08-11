set session group_concat_max_len = 20000;
SET @sql = NULL;
SET @patientAttributePivot = NULL;
SELECT
  GROUP_CONCAT(
      CONCAT('GROUP_CONCAT(DISTINCT (IF(person_attribute_type.name = \'', name, '\', IFNULL(person_attribute_cn.name, person_attribute.value), NULL))) as \'', name, '\''))
into @patientAttributePivot
FROM person_attribute_type where name in (#patientAttributes#);

SET @conceptObsPivot = NULL;
SELECT
  GROUP_CONCAT(
      CONCAT('GROUP_CONCAT(DISTINCT(IF(obs_value.obs_name = \'', name, '\', obs_value.value, NULL))) as \'', name, '\''))
into @conceptObsPivot
FROM concept_name where concept_name.name in (#conceptNames#) and concept_name_type = 'FULLY_SPECIFIED';

SET @sql = CONCAT('SELECT
      visit_attribute.date_created                                               AS "Date of Admission",
      vt.name                                                                    AS "Visit Type",
      visit_attribute.date_changed                                               AS "Date of Discharge",
      pi.identifier                                                              AS "Patient ID",
      CONCAT(pn.given_name, " ", pn.family_name)                                 AS "Patient Name",
      p.gender                                                                   AS "Gender",
      floor(datediff(#filterColumn#, p.birthdate) / 365)                         AS "Age",',
      @patientAttributePivot,
      ',',
      @conceptObsPivot ,
      ', GROUP_CONCAT(DISTINCT (diagnoses.diagnosis_name) SEPARATOR \' | \') AS "Diagnosis",
      pa.*
    FROM visit_attribute
    INNER JOIN visit_attribute_type vat
      ON vat.visit_attribute_type_id = visit_attribute.attribute_type_id
      AND vat.name = "Admission Status"
      AND CAST(#filterColumn# as DATE) BETWEEN "#startDate#" AND "#endDate#"
    INNER JOIN visit v
      ON v.visit_id = visit_attribute.visit_id
    INNER JOIN visit_type vt on v.visit_type_id = vt.visit_type_id
    INNER JOIN patient_identifier pi ON pi.patient_id = v.patient_id
    INNER JOIN person p ON p.person_id = v.patient_id
    INNER JOIN person_name pn ON pn.person_id = v.patient_id
    INNER JOIN person_address pa ON pa.person_id = v.patient_id
    INNER JOIN encounter e ON e.visit_id = v.visit_id
    LEFT JOIN person_attribute ON person_attribute.person_id = p.person_id
    LEFT JOIN person_attribute_type ON person_attribute_type.person_attribute_type_id = person_attribute.person_attribute_type_id
    LEFT JOIN concept_name person_attribute_cn
      ON person_attribute.value = person_attribute_cn.concept_id AND person_attribute_type.format LIKE "%Concept"
      AND person_attribute_cn.concept_name_type = "FULLY_SPECIFIED" AND person_attribute_type.name IN (#patientAttributes#)
    LEFT JOIN (SELECT
                 COALESCE(coded_diagnosis_concept_name.name, diagnosis_obs.value_text)       diagnosis_name,
                 diagnosis_obs.encounter_id     encounter_id
               FROM obs revised_obs
               JOIN concept_name revised_concept
                 ON revised_obs.concept_id = revised_concept.concept_id
                    AND revised_concept.concept_name_type = "FULLY_SPECIFIED"
                    AND revised_concept.name = "Bahmni Diagnosis Revised"
                    AND revised_obs.voided IS FALSE
                    AND revised_concept.voided IS FALSE

               JOIN obs diagnosis_obs
                 ON revised_obs.obs_group_id = diagnosis_obs.obs_group_id
               JOIN concept_name coded_diagnosis_concept
                 ON diagnosis_obs.concept_id = coded_diagnosis_concept.concept_id
                    AND coded_diagnosis_concept.name in ("Coded Diagnosis", "Non-coded Diagnosis")
                    AND coded_diagnosis_concept.concept_name_type = "FULLY_SPECIFIED"
                    AND coded_diagnosis_concept.voided IS FALSE
               LEFT JOIN concept_name coded_diagnosis_concept_name
                 ON diagnosis_obs.value_coded = coded_diagnosis_concept_name.concept_id
                    AND coded_diagnosis_concept_name.concept_name_type = "FULLY_SPECIFIED"
                    AND coded_diagnosis_concept_name.voided IS FALSE

               JOIN obs diagnosis_status_obs
                 ON revised_obs.obs_group_id = diagnosis_status_obs.obs_group_id
               JOIN concept_name diagnosis_status_concept
                 ON diagnosis_status_obs.concept_id = diagnosis_status_concept.concept_id
                    AND diagnosis_status_concept.concept_name_type = "FULLY_SPECIFIED"
                    AND diagnosis_status_concept.name = "Bahmni Diagnosis Status"
                    AND diagnosis_status_obs.voided IS FALSE
                    AND diagnosis_status_concept.voided IS FALSE
                    AND diagnosis_status_obs.value_coded IS NULL
               JOIN obs diagnosis_certainty_obs
                 ON revised_obs.obs_group_id = diagnosis_certainty_obs.obs_group_id
               JOIN concept_name diagnosis_certainty_concept
                 ON diagnosis_certainty_obs.concept_id = diagnosis_certainty_concept.concept_id
                    AND diagnosis_certainty_concept.concept_name_type = "FULLY_SPECIFIED"
                    AND diagnosis_certainty_concept.name = "Diagnosis Certainty"
                    AND diagnosis_certainty_obs.voided IS FALSE
                    AND diagnosis_certainty_concept.voided IS FALSE
               JOIN concept_name confirmed_concept
                 ON diagnosis_certainty_obs.value_coded = confirmed_concept.concept_id
                    AND confirmed_concept.concept_name_type = "FULLY_SPECIFIED"
                    AND confirmed_concept.name = "Confirmed"
                    AND confirmed_concept.voided IS FALSE
              ) diagnoses
        ON e.encounter_id = diagnoses.encounter_id
      LEFT JOIN (
          Select encounter_id, concept_name.name as obs_name, coalesce(obs.value_numeric, obs.value_boolean, obs.value_datetime, obs.value_text, coded_value.name) as value
            FROM obs
          INNER JOIN concept_name on obs.concept_id = concept_name.concept_id and concept_name_type = "FULLY_SPECIFIED"
          LEFT JOIN concept_name as coded_value on obs.value_coded is not null and obs.value_coded = coded_value.concept_id and coded_value.concept_name_type = "FULLY_SPECIFIED"
            WHERE concept_name.name in (#conceptNames#)) obs_value
        ON obs_value.encounter_id = e.encounter_id
  GROUP BY visit_attribute.date_created, pi.identifier, pn.given_name, pn.family_name, p.birthdate');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
