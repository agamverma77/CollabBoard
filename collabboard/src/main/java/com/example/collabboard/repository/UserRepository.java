    package com.example.collabboard.repository;

    import com.example.collabboard.model.User;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.stereotype.Repository;
    import java.util.Optional;

    @Repository
    public interface UserRepository extends JpaRepository<User, Long> {
        // Spring Data JPA automatically creates the query from the method name
        Optional<User> findByUsername(String username);
         Optional<User> findByEmail(String email);
    }
    
