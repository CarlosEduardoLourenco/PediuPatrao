package com.umc.pediupatrao.config;

import java.math.BigDecimal;
import java.util.List;
import org.bson.types.Decimal128;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

@Configuration
public class MongoConversoesConfig {

    @Bean
    public MongoCustomConversions conversoesMongo() {
        return new MongoCustomConversions(List.of(
                new BigDecimalParaDecimal128(),
                new Decimal128ParaBigDecimal()));
    }

    @WritingConverter
    private static class BigDecimalParaDecimal128 implements Converter<BigDecimal, Decimal128> {

        @Override
        public Decimal128 convert(BigDecimal valor) {
            return new Decimal128(valor);
        }
    }

    @ReadingConverter
    private static class Decimal128ParaBigDecimal implements Converter<Decimal128, BigDecimal> {

        @Override
        public BigDecimal convert(Decimal128 valor) {
            return valor.bigDecimalValue();
        }
    }
}
