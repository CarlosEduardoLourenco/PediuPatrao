package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.Funcionario;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FuncionarioRepository extends MongoRepository<Funcionario, String> {
}
