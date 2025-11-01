package com.example.ironplan.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name= "Exercises")
@Getter
@Setter
@AllArgsConstructor @NoArgsConstructor
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 100)
    @Column(nullable = false)
    private String name;

    @Column(nullable = false,  columnDefinition = "TEXT")
    private String description ;

    @Column(nullable = false,  columnDefinition = "TEXT")
    private String instructions ;

    @Column(nullable = false)
    private String primaryMuscle;

    @Column(nullable = true)
    private String secondaryMuscle;

    @Column(nullable = true)
    private String videoUrl;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();

    }

    @Column (nullable = false,  updatable = false)
    private LocalDateTime createdAt;

}
