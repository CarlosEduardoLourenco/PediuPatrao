package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.RegistroAuditoria;
import org.springframework.data.repository.Repository;

public interface AuditoriaRepository extends Repository<RegistroAuditoria, String> {

    RegistroAuditoria save(RegistroAuditoria registro);
}
