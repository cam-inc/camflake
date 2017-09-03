package com.camobile.camflake.sample;

import com.camobile.camflake.Camflake;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Component
@Path("id")
public class IdResource extends ResourceConfig {

    private Camflake camflake;

    public IdResource() {
        this.camflake = new Camflake();

        register(IdResource.class);
    }

    @GET
    @Path("/next")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNext() {
        return Response
                .ok()
                .entity(camflake.next())
                .build();
    }
}
