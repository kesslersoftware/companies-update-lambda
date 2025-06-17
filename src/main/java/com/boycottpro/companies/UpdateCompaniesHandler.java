package com.boycottpro.companies;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.boycottpro.models.Companies;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * update company metadata
 *
 * this lambda will only be called by admins via postman, to update the details about a company
 */
public class UpdateCompaniesHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String TABLE_NAME = "";
    private final DynamoDbClient dynamoDb;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UpdateCompaniesHandler() {
        this.dynamoDb = DynamoDbClient.create();
    }

    public UpdateCompaniesHandler(DynamoDbClient dynamoDb) {
        this.dynamoDb = dynamoDb;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        try {
            Companies input = objectMapper.readValue(event.getBody(), Companies.class);
            boolean validInput = checkCompany(input);
            if(!validInput) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("{\"error\":\"Invalid input\"}");
            }
            boolean updatedCompany = updateCompany(input);
            if(!updatedCompany) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(500)
                        .withBody("{\"error\": \"failed to update the company\"}");
            }
            String responseBody = objectMapper.writeValueAsString(input);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(responseBody);
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("{\"error\": \"Unexpected server error: " + e.getMessage() + "\"}");
        }
    }

    private boolean checkCompany(Companies company) {
        if(company.getCompany_id()==null || company.getCompany_id().isEmpty() ||
        company.getCompany_name()==null || company.getCompany_name().isEmpty() ||
        company.getCeo()==null || company.getCeo().isEmpty() ||
        company.getCity()==null || company.getCity().isEmpty() ||
        company.getIndustry()==null || company.getIndustry().isEmpty() ||
        company.getState()==null || company.getState().isEmpty() ||
        company.getBoycott_count()<0 ||
        company.getEmployees()<0 ||
        company.getProfits()<0 ||
        company.getValuation()<0 ||
        company.getRevenue()<0 ||
        company.getZip()==null || company.getZip().isEmpty()) {
            return false;
        }
        return true;
    }
    private boolean updateCompany(Companies company) {
        String companyId = company.getCompany_id();

        // Step 1: Check if the company exists
        GetItemRequest getRequest = GetItemRequest.builder()
                .tableName("companies")
                .key(Map.of("company_id", AttributeValue.fromS(companyId)))
                .build();

        GetItemResponse getResponse = dynamoDb.getItem(getRequest);
        if (!getResponse.hasItem()) {
            return false; // Company not found
        }

        // Step 2: Build the UpdateItemRequest
        UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                .tableName("companies")
                .key(Map.of("company_id", AttributeValue.fromS(companyId)))
                .updateExpression("SET " +
                        "company_name = :name, description = :desc, industry = :ind, city = :city, #st = :state, zip = :zip, " +
                        "employees = :emp, revenue = :rev, valuation = :val, profits = :prof, " +
                        "stock_symbol = :symbol, ceo = :ceo")
                .expressionAttributeNames(Map.of(
                        "#st", "state"
                ))
                .expressionAttributeValues(Map.ofEntries(
                        Map.entry(":name", AttributeValue.fromS(company.getCompany_name())),
                        Map.entry(":desc", AttributeValue.fromS(company.getDescription())),
                        Map.entry(":ind", AttributeValue.fromS(company.getIndustry())),
                        Map.entry(":city", AttributeValue.fromS(company.getCity())),
                        Map.entry(":state", AttributeValue.fromS(company.getState())),  // value still uses original name
                        Map.entry(":zip", AttributeValue.fromS(company.getZip())),
                        Map.entry(":emp", AttributeValue.fromN(Integer.toString(company.getEmployees()))),
                        Map.entry(":rev", AttributeValue.fromN(Long.toString(company.getRevenue()))),
                        Map.entry(":val", AttributeValue.fromN(Long.toString(company.getValuation()))),
                        Map.entry(":prof", AttributeValue.fromN(Long.toString(company.getProfits()))),
                        Map.entry(":symbol", AttributeValue.fromS(company.getStock_symbol())),
                        Map.entry(":ceo", AttributeValue.fromS(company.getCeo()))
                ))
                .build();

        // Step 3: Execute the update
        dynamoDb.updateItem(updateRequest);
        return true;
    }

}