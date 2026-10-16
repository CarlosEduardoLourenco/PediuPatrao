package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.HorarioAtendimento;
import java.time.DayOfWeek;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HorarioAtendimentoRepository extends MongoRepository<HorarioAtendimento, String> {

    Optional<HorarioAtendimento> findByDiaDaSemana(DayOfWeek diaDaSemana);
}
