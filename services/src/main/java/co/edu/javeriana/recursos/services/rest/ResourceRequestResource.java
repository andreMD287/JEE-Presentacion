package co.edu.javeriana.recursos.services.rest;

import co.edu.javeriana.recursos.contracts.ResourceRequestService;
import co.edu.javeriana.recursos.contracts.dto.CreateResourceRequestCommand;
import co.edu.javeriana.recursos.contracts.dto.ResourceRequestView;
import co.edu.javeriana.recursos.services.client.LogicClient;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Context;

import java.net.URI;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.Function;

/** Fachada HTTP de las cinco operaciones. Las validaciones de dominio viven solo en Lógica. */
@Path("/requests")
@Produces(MediaType.APPLICATION_JSON)
public class ResourceRequestResource {
    private final Supplier<ResourceRequestService> services;
    private final Function<String, CreateResourceRequestCommand> decoder;

    @Context
    UriInfo uriInfo;

    public ResourceRequestResource() {
        this(LogicClient::get, ResourceRequestResource::decodeJson);
    }

    ResourceRequestResource(Supplier<ResourceRequestService> services,
                            Function<String, CreateResourceRequestCommand> decoder) {
        this.services = services;
        this.decoder = decoder;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(String body) {
        if (body == null || body.isBlank()) {
            throw new PayloadException("EMPTY_BODY", "El cuerpo de la solicitud es obligatorio");
        }
        CreateResourceRequestCommand command;
        try {
            command = decoder.apply(body);
        } catch (JsonbException e) {
            throw new PayloadException("MALFORMED_JSON", "El JSON enviado no es válido");
        }
        // El JSON 'null' no crea un registro; Lógica produce la misma validación del T2.
        ResourceRequestView registered = services.get().register(command);
        URI location = uriInfo == null
                ? URI.create("/services/api/v1/requests/" + registered.getId())
                : uriInfo.getAbsolutePathBuilder().path(String.valueOf(registered.getId())).build();
        return Response.created(location).entity(registered).build();
    }

    private static CreateResourceRequestCommand decodeJson(String body) {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            return jsonb.fromJson(body, CreateResourceRequestCommand.class);
        } catch (JsonbException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("No se pudo procesar JSON", error);
        }
    }

    @GET
    public List<ResourceRequestView> list() {
        return services.get().findAll();
    }

    @GET
    @Path("/pending")
    public List<ResourceRequestView> pending() {
        return services.get().findPending();
    }

    @GET
    @Path("/{id}")
    public ResourceRequestView get(@PathParam("id") Long id) {
        return services.get().findById(id);
    }

    @PUT
    @Path("/{id}/attend")
    public ResourceRequestView attend(@PathParam("id") Long id) {
        return services.get().attend(id);
    }
}
