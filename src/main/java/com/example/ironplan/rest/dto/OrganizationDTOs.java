package com.example.ironplan.rest.dto;
 
import com.example.ironplan.model.OrganizationKind;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
 
import java.util.ArrayList;
import java.util.List;
 
public class OrganizationDTOs {
 
    // ─── Request: crear/editar jerarquía completa ─────────────────────────────
 
    @Getter @Setter
    public static class CreateRequest {
 
        @NotBlank(message = "El nombre es obligatorio")
        private String name;
 
        private OrganizationKind organizationKind; // solo en raíz
 
        private List<NodeRequest> children = new ArrayList<>();
    }
 
    @Getter @Setter
    public static class NodeRequest {
 
        @NotBlank(message = "El nombre del subgrupo es obligatorio")
        private String name;
 
        // Si viene con id → es un nodo existente (editar)
        // Si no tiene id  → es nuevo (crear)
        private Long id;
 
        private List<NodeRequest> children = new ArrayList<>();
    }
 
    // ─── Request: editar jerarquía existente ──────────────────────────────────
 
    @Getter @Setter
    public static class UpdateRequest {
 
        @NotBlank
        private String name;
 
        private OrganizationKind organizationKind;
 
        // Nodos con id = actualizar, sin id = crear nuevos
        // Nodos que ya existían pero no vienen = desactivar
        private List<NodeRequest> children = new ArrayList<>();
    }
 
    // ─── Response: árbol completo de una org ──────────────────────────────────
 
    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class NodeResponse {
        private Long   id;
        private String name;
        private String code;
        private String groupType;     // EMPRESA, FACULTAD, CARRERA, GRUPO
        private OrganizationKind organizationKind;
        private Boolean active;
        private List<NodeResponse> children;
    }
}