package co.edu.javeriana.recursos.data.ejb;

import co.edu.javeriana.recursos.contracts.ResourceRequestDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataWarmupTest {

    private ResourceRequestDataService data;
    private DataWarmup warmup;

    @BeforeEach
    void setUp() {
        data = mock(ResourceRequestDataService.class);
        warmup = new DataWarmup();
        warmup.data = data;
    }

    @Test
    void warmUpRunsTheListingAndLookupPaths() {
        when(data.findAll()).thenReturn(List.of());

        warmup.warmUp();

        verify(data).findAll();
        verify(data).findById(0L);
    }

    @Test
    void aFailedWarmUpDoesNotStopTheDeployment() {
        when(data.findAll()).thenThrow(new IllegalStateException("PostgreSQL no responde"));

        assertDoesNotThrow(warmup::warmUp);
    }
}
