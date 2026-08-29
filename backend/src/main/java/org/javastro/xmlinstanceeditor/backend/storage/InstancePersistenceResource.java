package org.javastro.xmlinstanceeditor.backend.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.javastro.xmlinstanceeditor.api.storage.EditedInstance;
import org.javastro.xmlinstanceeditor.api.storage.FileInstanceStore;
import org.javastro.xmlinstanceeditor.api.storage.InstanceStore;

import java.util.List;
import java.util.Map;

@Path("/instances")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class InstancePersistenceResource {
    private final InstanceStore<Map<String, Object>> store;

    public InstancePersistenceResource(ObjectMapper objectMapper, @ConfigProperty(name = "xmlinstanceeditor.storage.root-dir") java.nio.file.Path rootDir) {
        this.store = new FileInstanceStore(objectMapper, rootDir);
    }

    @POST
    public EditedInstance<Map<String, Object>> save(EditedInstance<Map<String, Object>> request) {
        return store.save(request);
    }

    @GET
    @Path("/{id}")
    public EditedInstance<Map<String, Object>> get(@PathParam("id") String id) {
        return store.findById(id).orElseThrow(() -> new jakarta.ws.rs.NotFoundException("Instance not found: " + id));
    }

    @GET
    public List<EditedInstance<Map<String, Object>>> list() {
        return store.findAll();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        return store.delete(id) ? Response.noContent().build() : Response.status(Response.Status.NOT_FOUND).build();
    }
}
