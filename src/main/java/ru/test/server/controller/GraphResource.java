package ru.test.server.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ru.test.data.migration.MigrationEngine;
import ru.test.domain.exception.CsvParserException;
import ru.test.domain.exception.MigrationException;
import ru.test.server.dto.CreateProfileRequest;
import ru.test.server.dto.ProfileResponse;
import ru.test.server.dto.UpdateProfileRequest;
import ru.test.server.service.GraphService;

import java.net.URI;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class GraphResource {

    @Inject
    GraphService service;

    @GET
    @Path("/profiles")
    public Response getAll(@QueryParam("city") String city) {
        return Response.ok(service.getAllProfiles(city)).build();
    }

    @GET
    @Path("/profiles/{id}")
    public Response getOne(@PathParam("id") int id) {
        return Response.ok(service.getProfileById(id)).build();
    }

    @POST
    @Path("/profiles")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(CreateProfileRequest request) {
        ProfileResponse created = service.createProfile(request);
        return Response.created(URI.create("/api/profiles/" + created.id()))
                .entity(created)
                .build();
    }

    @PUT
    @Path("/profiles/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") int id, UpdateProfileRequest request) throws CsvParserException {
        return Response.ok(service.updateProfile(id, request)).build();
    }

    @DELETE
    @Path("/profiles/{id}")
    public Response delete(@PathParam("id") int id) {
        service.deleteProfile(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/profiles/{id}/friends")
    public Response addFriend(@PathParam("id") int id,
                              @QueryParam("target") int targetId,
                              @QueryParam("strength") @DefaultValue("1") int strength) {
        return Response.ok(service.addFriendship(id, targetId, strength)).build();
    }

    @GET
    @Path("/profiles/path")
    public Response getShortestPath(@QueryParam("from") int from, @QueryParam("to") int to) {
        return Response.ok(service.findShortestPath(from, to)).build();
    }

    @POST
    @Path("/generate")
    public Response generate(@QueryParam("count") @DefaultValue("10") int count) {
        return Response.status(Response.Status.CREATED)
                .entity(service.generateData(count))
                .build();
    }

    @POST
    @Path("/export")
    public Response exportData(@QueryParam("format") @DefaultValue("json") String format,
                               @QueryParam("version") @DefaultValue("3") int version) throws MigrationException {
        MigrationEngine.DowngradeWarning warning = MigrationEngine.getDowngradeWarning(version);

        if ("proto".equalsIgnoreCase(format) || "protobuf".equalsIgnoreCase(format)) {
            byte[] bytes = service.exportProtobuf(version);
            return Response.ok(bytes, "application/x-protobuf")
                    .header("Content-Disposition", "attachment; filename=\"graph-v" + version + ".pb\"")
                    .header("X-Downgrade-Warning", warning.message())
                    .build();
        }

        if ("json".equalsIgnoreCase(format)) {
            String json = service.exportJson(version);
            return Response.ok(json, MediaType.APPLICATION_JSON)
                    .header("X-Downgrade-Warning", warning.message())
                    .build();
        }

        throw new IllegalArgumentException("Неподдерживаемый формат экспорта: " + format + ". Доступны: json, proto");
    }

    @POST
    @Path("/import")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response importJson(String jsonPayload) throws MigrationException {
        return Response.ok(service.importJson(jsonPayload)).build();
    }

    @POST
    @Path("/import")
    @Consumes({"application/x-protobuf", MediaType.APPLICATION_OCTET_STREAM})
    public Response importProtobuf(byte[] protoPayload) throws MigrationException {
        return Response.ok(service.importProtobuf(protoPayload)).build();
    }
}