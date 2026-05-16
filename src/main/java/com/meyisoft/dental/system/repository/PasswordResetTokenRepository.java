package com.meyisoft.dental.system.repository;

import com.meyisoft.dental.system.entity.PasswordResetToken;
import com.meyisoft.dental.system.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    @Query("SELECT p FROM PasswordResetToken p JOIN FETCH p.usuario WHERE p.token = :token")
    Optional<PasswordResetToken> findByTokenWithUsuario(String token);

    @Modifying
    @Query("DELETE FROM PasswordResetToken p WHERE p.usuario = :usuario")
    void deleteByUsuario(Usuario usuario);
}
