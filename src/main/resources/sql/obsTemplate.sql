set session group_concat_max_len = 20000;
SET @sql = NULL;
SELECT
  GROUP_CONCAT(DISTINCT
               CONCAT(
                   'GROUP_CONCAT(DISTINCT(IF(cv.concept_full_name = ''',
                   concept_full_name,
                   ''', coalesce(o.value_numeric, o.value_boolean, o.value_datetime, o.value_text, o.concept_short_name, o.concept_full_name), NULL)) SEPARATOR \',\') AS `',
                   concept_full_name , '`'
               )
  ) into @sql
FROM concept_view cv where cv.concept_full_name in (%s);

SET @sql = CONCAT('SELECT
                  o.identifier,
                  o.patient_name,
                  o.age,
                  o.gender,
                  o.provider_id,
                  o.encounter_id,
                  GROUP_CONCAT(DISTINCT(o.provider_name) SEPARATOR \',\') as provider_name,
                  o.encounter_datetime,',
                 @sql,
                  ' FROM concept_view cv
                    LEFT JOIN
                    (SELECT
                       pi.identifier,
                       ob.concept_id,
                       concat(pat_name.given_name, '' '', pat_name.family_name) AS patient_name,
                       floor(DATEDIFF(DATE(ob.obs_datetime), person.birthdate) / 365)   AS age,
                       person.gender,
                       ep.provider_id,
                       ep.encounter_id,
                       concat(pn.given_name, '' '', pn.family_name)   AS provider_name,
                       e.encounter_datetime,
                       ob.value_numeric,
                       ob.value_boolean,
                       ob.value_datetime,
                       ob.value_text,
                       answer.concept_short_name,
                       answer.concept_full_name
                     FROM obs ob
                       JOIN encounter e ON ob.encounter_id = e.encounter_id AND cast(e.encounter_datetime AS DATE) BETWEEN \'%s\' AND \'%s\'  AND ob.voided IS FALSE
                       JOIN (select encounter_id from obs
                                            where concept_id = (select concept_id from concept_view where concept_full_name = \'%s\') and voided is false) e1
                       ON e.encounter_id = e1.encounter_id
                       JOIN patient_identifier pi ON pi.patient_id = ob.person_id
                       JOIN person ON person.person_id = pi.patient_id
                       JOIN person_name pat_name ON pat_name.person_id = person.person_id
                       JOIN encounter_provider ep ON e.encounter_id = ep.encounter_id
                       JOIN provider p ON ep.provider_id = p.provider_id
                       JOIN person_name pn ON p.person_id = pn.person_id
                       LEFT JOIN concept_view answer ON ob.value_coded = answer.concept_id
                       ) o ON o.concept_id = cv.concept_id
                  where cv.concept_full_name IN ( %s )
                  group by identifier, encounter_id
                  order by identifier, encounter_id');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

