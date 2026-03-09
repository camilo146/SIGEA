package co.edu.sena.sigea.categoria.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.sena.sigea.categoria.entity.Categoria;

@Repository
//esto es un repositorio de categorias, se encarga de hacer las consultas a la base de datos para obtener las categorias,
// se pueden hacer consultas por nombre, por id, etc.
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    Optional<Categoria> findByNombre(String nombre);

    boolean existsByNombre(String nombre);

    List<Categoria> findByActivoTrue();
}