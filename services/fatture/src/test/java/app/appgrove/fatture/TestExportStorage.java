package app.appgrove.fatture;

import app.appgrove.commons.storage.InMemoryExportStorage;
import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;

/** Storage export in-memory nei test (sostituisce {@code S3ExportStorage}): niente MinIO. */
@Mock
@ApplicationScoped
public class TestExportStorage extends InMemoryExportStorage {}
