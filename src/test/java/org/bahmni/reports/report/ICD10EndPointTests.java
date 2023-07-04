package org.bahmni.reports.report;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bahmni.reports.icd10.Impl.Icd10ServiceImpl;
import org.bahmni.reports.icd10.bean.ICDRule;
import org.bahmni.webclients.HttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.doReturn;

@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*"})
@RunWith(PowerMockRunner.class)
@PrepareForTest(Icd10ServiceImpl.class)
public class ICD10EndPointTests {

    @Mock
    private HttpURLConnection mockConnection;

    @Mock
    private Icd10ServiceImpl icd10ServiceImpl;
    @Mock
    private HttpClient mockHttpClient;

    @InjectMocks
    private Icd10ServiceImpl icd10Service;

    @Mock
    private ObjectMapper mapper;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testTests() {
        System.out.println("Thi is working!!");
    }

    @Test
    public void testGetMapRules() throws IOException {
        // Arrange
        String snomedCode = "123456";
        Integer offset = 0;
        Integer limit = 10;
        Boolean termActive = true;

        Icd10ServiceImpl icd10Service = new Icd10ServiceImpl();
        String baseURL = "https://browser.ihtsdotools.org/snowstorm/snomed-ct/MAIN/SNOMEDCT-ES/2022-10-31/concepts";
        String eclUrl = "^[*] 447562003 |ICD-10 complex map reference set| {{ M referencedComponentId = \"" + snomedCode + "\" }}";

        String encodedEcl = "encodedEcl";
        String urls = "https://browser.ihtsdotools.org/snowstorm/snomed-ct/MAIN/SNOMEDCT-ES/2022-10-31/concepts?offset=0&limit=10&termActive=true&ecl=" + encodedEcl;
        String response = "{\"items\": [{\"mapGroup\": \"1\", \"mapPriority\": \"2\"}, {\"mapGroup\": \"2\", \"mapPriority\": \"1\"}]}";
        ObjectMapper mapper = new ObjectMapper();


        when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(mockConnection.getInputStream()).thenReturn(new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)));


        //when(icd10Service.encode(anyString())).thenReturn(encodedEcl);
     //   when(mapper.readValue(eq(response), eq(JsonNode.class))).thenReturn(mock(JsonNode.class));
      //  when(mapper.writeValueAsString(any())).thenReturn(response);

       // List<ICDRule> sortedRules = new ArrayList<>();
//        sortedRules.add(2, "1");
        List<ICDRule> actualSortedRules = icd10Service.getMapRules(snomedCode, offset, limit, termActive);
        assertEquals(2, actualSortedRules.size());


      //  verify(mockConnection, times(1)).setRequestMethod("GET");
   //     verify(mockConnection, times(1)).getResponseCode();
   //     verify(mockConnection, times(1)).getInputStream();
       // verify(icd10Service, times(1)).encode(anyString());
    //    verify(mapper, times(1)).readValue(eq(response), eq(JsonNode.class));
    //    verify(mapper, times(1)).writeValueAsString(any());
    }






}