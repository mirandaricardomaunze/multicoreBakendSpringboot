package mz.multicore.erp.modules.units.service;

import mz.multicore.erp.modules.units.model.UnitOfMeasure;
import mz.multicore.erp.modules.units.repository.UnitOfMeasureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures at start-up that the system units exist. Idempotent: it only inserts missing codes and never
 * updates or deletes existing rows. It behaves the same on H2 (without Flyway) and on PostgreSQL.
 */
@Component
public class SystemUnitCatalogSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SystemUnitCatalogSeeder.class);

    private final UnitOfMeasureRepository unitRepository;

    public SystemUnitCatalogSeeder(UnitOfMeasureRepository unitRepository) {
        this.unitRepository = unitRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedMissing();
    }

    @Transactional
    public int seedMissing() {
        int created = 0;
        for (SystemUnitCatalog.Definition def : SystemUnitCatalog.units()) {
            if (unitRepository.findByCodeAndCompanyIsNull(def.code()).isPresent()) continue;
            UnitOfMeasure unit = new UnitOfMeasure();
            unit.setCode(def.code());
            unit.setName(def.name());
            unit.setSymbol(def.symbol());
            unit.setDimension(def.dimension());
            unit.setDimensionFactor(def.dimensionFactor());
            unit.setDecimals(def.decimals());
            unit.setSystemUnit(true);
            unit.setActive(true);
            unitRepository.save(unit);
            created++;
        }
        if (created > 0) log.info("Unidades de medida do sistema criadas: {}", created);
        return created;
    }
}
