package cubeGame.demo.config;

import cubeGame.demo.exception.ErrorResponseDto;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gameAppOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cube Game API")
                        .version("1.0")
                        .description("REST API documentation for Cube Game application"));
    }

    @Bean
    public OpenApiCustomizer globalGameErrorResponsesCustomizer() {
        return openApi -> {
            // 1. Register ErrorResponseDto in the OpenAPI component schema
            var resolvedSchema = ModelConverters.getInstance()
                    .resolveAsResolvedSchema(new AnnotatedType(ErrorResponseDto.class));
            openApi.schema("ErrorResponseDto", resolvedSchema.schema);

            // 2. Reference the ErrorResponseDto schema
            Schema<?> errorSchemaRef = new Schema<>().$ref("#/components/schemas/ErrorResponseDto");
            Content errorContent = new Content().addMediaType(
                    org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                    new MediaType().schema(errorSchemaRef)
            );

            // 3. Define the responses matching your GlobalExceptionHandler
            ApiResponse badRequest = new ApiResponse()
                    .description("Bad Request: Invalid move, invalid position, or illegal argument")
                    .content(errorContent);

            ApiResponse notFound = new ApiResponse()
                    .description("Not Found: Game or resource does not exist")
                    .content(errorContent);

            ApiResponse conflict = new ApiResponse()
                    .description("Conflict: Illegal game state (e.g. game already finished or out-of-turn play)")
                    .content(errorContent);

            ApiResponse internalError = new ApiResponse()
                    .description("Internal Server Error")
                    .content(errorContent);

            // 4. Attach responses across all endpoints
            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(operation -> {
                        operation.getResponses().addApiResponse("400", badRequest);
                        operation.getResponses().addApiResponse("404", notFound);
                        operation.getResponses().addApiResponse("409", conflict);
                        operation.getResponses().addApiResponse("500", internalError);
                    })
            );
        };
    }
}