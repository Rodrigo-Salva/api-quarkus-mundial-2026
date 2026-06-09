package com.mundial2026.predictions.auth.controller;

import com.mundial2026.predictions.auth.entity.User;
import com.mundial2026.predictions.auth.repository.UserRepository;
import com.mundial2026.predictions.shared.response.ApiResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@Path("/api/admin/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Admin - Users")
public class UserAdminResource {

    @Inject UserRepository userRepository;
    @Inject EntityManager em;

    record UserDTO(Long id, String email, String username, String country, String role, String createdAt) {
        static UserDTO from(User u) {
            return new UserDTO(u.id, u.email, u.username, u.country, u.role,
                    u.createdAt != null ? u.createdAt.toString() : null);
        }
    }

    @GET
    public Response getAll() {
        List<UserDTO> users = userRepository.listAll()
                .stream().map(UserDTO::from).toList();
        return Response.ok(ApiResponse.ok(users)).build();
    }

    @PUT
    @Path("/{id}/role")
    @Transactional
    public Response updateRole(@PathParam("id") Long id, Map<String, String> body) {
        User user = userRepository.findById(id);
        if (user == null) return Response.status(404).entity(ApiResponse.error("Usuario no encontrado")).build();
        String newRole = body.get("role");
        if (!"USER".equals(newRole) && !"ADMIN".equals(newRole))
            return Response.status(400).entity(ApiResponse.error("Rol inválido")).build();
        user.role = newRole;
        return Response.ok(ApiResponse.ok(UserDTO.from(user))).build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        User user = userRepository.findById(id);
        if (user == null) return Response.status(404).entity(ApiResponse.error("Usuario no encontrado")).build();

        // Eliminar registros relacionados en orden para respetar FK constraints
        em.createNativeQuery("DELETE FROM accumulator_bet_selections WHERE accumulator_bet_id IN (SELECT id FROM accumulator_bets WHERE user_id = :uid)").setParameter("uid", id).executeUpdate();
        em.createNativeQuery("DELETE FROM accumulator_bets WHERE user_id = :uid").setParameter("uid", id).executeUpdate();
        em.createNativeQuery("DELETE FROM bets WHERE user_id = :uid").setParameter("uid", id).executeUpdate();
        em.createNativeQuery("DELETE FROM wallet_transactions WHERE wallet_id IN (SELECT id FROM wallets WHERE user_id = :uid)").setParameter("uid", id).executeUpdate();
        em.createNativeQuery("DELETE FROM wallets WHERE user_id = :uid").setParameter("uid", id).executeUpdate();
        em.createNativeQuery("DELETE FROM self_exclusions WHERE user_id = :uid").setParameter("uid", id).executeUpdate();
        em.createNativeQuery("DELETE FROM gambling_limits WHERE user_id = :uid").setParameter("uid", id).executeUpdate();
        em.createNativeQuery("DELETE FROM kyc_verifications WHERE user_id = :uid").setParameter("uid", id).executeUpdate();
        em.createNativeQuery("DELETE FROM league_members WHERE user_id = :uid").setParameter("uid", id).executeUpdate();
        em.createNativeQuery("DELETE FROM leagues WHERE owner_id = :uid").setParameter("uid", id).executeUpdate();
        em.createNativeQuery("DELETE FROM predictions WHERE user_id = :uid").setParameter("uid", id).executeUpdate();

        userRepository.delete(user);
        return Response.noContent().build();
    }
}
