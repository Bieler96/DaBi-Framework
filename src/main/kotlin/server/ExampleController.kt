package server

import auth.User
import kotlinx.serialization.Serializable

@Serializable
data class MyRequestBody(val name: String, val age: Int)

@Serializable
data class MyResponseBody(val id: String, val message: String)

@Controller("/example")
class ExampleController {

    @GetMapping(
        path = "/{id}",
        summary = "Get an entity by ID",
        description = "This endpoint retrieves a specific entity by its unique identifier."
    )
    @PathParameter(name = "id", description = "The unique identifier of the entity")
    @ApiResponse(
        statusCode = 200,
        description = "Successful response",
        content = [Content(schema = MyResponseBody::class)]
    )
    @ApiResponse(statusCode = 404, description = "Entity not found")
    fun getById(
        @RequestParam(name = "id") id: String,
        @RequestParam(name = "verbose", required = false, description = "Enable verbose output") verbose: Boolean = false
    ): MyResponseBody {
        return MyResponseBody(id = id, message = "Entity found with verbose=$verbose")
    }

    @PostMapping(
        path = "/create",
        summary = "Create a new entity",
        description = "This endpoint creates a new entity based on the provided data."
    )
    @ApiResponse(
        statusCode = 201,
        description = "Entity created successfully",
        content = [Content(schema = MyResponseBody::class)]
    )
    fun create(
        @RequestBody(description = "The data for the new entity") body: MyRequestBody
    ): MyResponseBody {
        return MyResponseBody(id = "new-id", message = "Created entity with name ${body.name}")
    }
}
