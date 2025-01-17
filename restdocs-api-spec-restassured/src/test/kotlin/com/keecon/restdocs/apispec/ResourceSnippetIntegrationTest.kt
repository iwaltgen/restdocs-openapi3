package com.keecon.restdocs.apispec

import com.keecon.restdocs.apispec.ResourceDocumentation.parameterWithName
import com.keecon.restdocs.apispec.ResourceDocumentation.resource
import org.hibernate.validator.constraints.Length
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.Link
import org.springframework.hateoas.server.mvc.BasicLinkBuilder
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.ResponseEntity
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.ResultActions
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.constraints.NotEmpty

@ExtendWith(SpringExtension::class)
@WebMvcTest
@AutoConfigureRestDocs
open class ResourceSnippetIntegrationTest {

    val operationName = "test-${System.currentTimeMillis()}"

    lateinit var resultActions: ResultActions

    private lateinit var app: TestApplication
    protected var serverPort: Int? = null

    @BeforeEach
    fun setUp() {
        app = TestApplication()
        app.main(arrayOf("--server.port=0"))
        serverPort = app.applicationContext.environment.getProperty("local.server.port")?.toInt()
    }

    @AfterEach
    fun tearDown() {
        app.applicationContext.close()
    }

    @SpringBootApplication
    open class TestApplication {
        lateinit var applicationContext: ConfigurableApplicationContext
        fun main(args: Array<String>) {
            applicationContext = SpringApplication.run(TestApplication::class.java, *args)
        }

        @RestController
        internal open class TestController {

            @PostMapping(path = ["/some/{someId}/other/{otherId}"])
            fun doSomething(
                @PathVariable someId: String,
                @PathVariable otherId: Int?,
                @RequestHeader("X-Custom-Header") customHeader: String,
                @RequestBody testDataHolder: TestDataHolder
            ): ResponseEntity<EntityModel<TestDataHolder>> {
                val resource = EntityModel.of(testDataHolder.copy(id = UUID.randomUUID().toString()))
                val link =
                    BasicLinkBuilder.linkToCurrentMapping().slash("some").slash(someId).slash("other").slash(otherId)
                        .toUri().toString()
                resource.add(Link.of(link).withSelfRel())
                resource.add(Link.of(link, "multiple"))
                resource.add(Link.of(link, "multiple"))

                return ResponseEntity
                    .ok()
                    .header("X-Custom-Header", customHeader)
                    .body<EntityModel<TestDataHolder>>(resource)
            }
        }
    }

    internal data class TestDataHolder(
        @field:Length(min = 1, max = 255)
        val comment: String? = null,
        val flag: Boolean = false,
        val count: Int = 0,
        @field:NotEmpty
        val id: String? = null
    )
}

fun fieldDescriptors(): FieldDescriptors {
    val fields = ConstrainedFields(ResourceSnippetIntegrationTest.TestDataHolder::class.java)
    return ResourceDocumentation.fields(
        fields.withPath("comment").description("the comment").optional(),
        fields.withPath("flag").description("the flag"),
        fields.withMappedPath("count", "count").description("the count")
    )
}

fun buildFullResourceSnippet(): ResourceSnippet {
    return resource(
        ResourceSnippetParameters.builder()
            .description("description")
            .summary("summary")
            .deprecated(true)
            .privateResource(true)
            .requestFields(fieldDescriptors())
            .responseFields(fieldDescriptors().and(fieldWithPath("id").description("id")))
            .requestHeaders(
                headerWithName("X-Custom-Header").description("A custom header"),
                headerWithName(ACCEPT).description("Accept")
            )
            .responseHeaders(
                headerWithName("X-Custom-Header").description("A custom header"),
                headerWithName(CONTENT_TYPE).description("ContentType")
            )
            .pathParameters(
                parameterWithName("someId").description("some id"),
                parameterWithName("otherId").description("otherId id").type(SimpleType.INTEGER)
            )
            .links(
                linkWithRel("self").description("some"),
                linkWithRel("multiple").description("multiple")
            )
            .build()
    )
}
