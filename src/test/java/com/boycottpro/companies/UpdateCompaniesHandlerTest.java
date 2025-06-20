package com.boycottpro.companies;

import com.amazonaws.services.lambda.runtime.Context;
import com.boycottpro.models.Companies;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UpdateCompaniesHandlerTest {

    @Mock
    private DynamoDbClient dynamoDb;

    @InjectMocks
    private UpdateCompaniesHandler handler;

    @Captor
    ArgumentCaptor<UpdateItemRequest> updateRequestCaptor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Companies buildValidCompany() {
        Companies c = new Companies();
        c.setCompany_id("c1");
        c.setCompany_name("Test Company");
        c.setCeo("Jane Doe");
        c.setCity("New York");
        c.setState("NY");
        c.setZip("10001");
        c.setIndustry("Tech");
        c.setDescription("Test Desc");
        c.setEmployees(500);
        c.setRevenue(100000L);
        c.setValuation(1000000L);
        c.setProfits(50000L);
        c.setStock_symbol("TST");
        c.setBoycott_count(5);
        return c;
    }

    @Test
    public void testValidUpdateReturns200() throws Exception {
        Companies company = buildValidCompany();

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withBody(objectMapper.writeValueAsString(company));

        when(dynamoDb.getItem(any(GetItemRequest.class)))
                .thenReturn(GetItemResponse.builder().item(Map.of("company_id", AttributeValue.fromS("c1"))).build());

        UpdateItemResponse updateResponse = UpdateItemResponse.builder().build();
       when(dynamoDb.updateItem(any(UpdateItemRequest.class))).thenReturn(updateResponse);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mock(Context.class));

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"company_id\":\"c1\""));
    }

    @Test
    public void testMissingCompanyIdReturns400() throws Exception {
        Companies invalidCompany = buildValidCompany();
        invalidCompany.setCompany_name("");

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withBody(objectMapper.writeValueAsString(invalidCompany));

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mock(Context.class));

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid input"));
    }

    @Test
    public void testCompanyNotFoundReturns500() throws Exception {
        Companies company = buildValidCompany();

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withBody(objectMapper.writeValueAsString(company));

        when(dynamoDb.getItem(any(GetItemRequest.class)))
                .thenReturn(GetItemResponse.builder().build());  // no item returned

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mock(Context.class));

        assertEquals(500, response.getStatusCode());
        assertTrue(response.getBody().contains("failed to update"));
    }

    @Test
    public void testExceptionReturns500() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withBody("this-is-not-json");

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mock(Context.class));

        assertEquals(500, response.getStatusCode());
        assertTrue(response.getBody().contains("Unexpected server error"));
    }
}
