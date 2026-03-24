package simulator.control;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import simulator.model.Simulator;
import simulator.model.region.MapInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test class for Controller, covering all public interface methods
 */
public class ControllerTest {

    @Mock
    private Simulator simulator;

    @Mock
    private MapInfo mapInfo;

    private Controller controller;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Configure simulator mock default behavior
        when(simulator.asJson()).thenReturn(new JSONObject().put("time", 0.0));
        when(simulator.getTime()).thenReturn(0.0);
        when(simulator.getAnimals()).thenReturn(new ArrayList<>());
        when(simulator.getMapInfo()).thenReturn(mapInfo);

        // Configure mapInfo mock
        when(mapInfo.getWidth()).thenReturn(600);
        when(mapInfo.getHeight()).thenReturn(600);
        when(mapInfo.getCols()).thenReturn(3);
        when(mapInfo.getRows()).thenReturn(3);

        controller = new Controller(simulator);
    }

    // ========== CONSTRUCTOR TESTS ==========

    @Test
    public void testValidConstructor() {
        // Verify: constructor creates instance
        Controller ctrl = new Controller(simulator);
        assertNotNull(ctrl, "Controller should be created");
    }

    @Test
    public void testConstructorNullSimulator() {
        // Verify: constructor throws exception when simulator is null
        assertThrows(IllegalArgumentException.class, () -> {
            new Controller(null);
        }, "Should throw exception when simulator is null");
    }

    // ========== LOAD DATA TESTS ==========

    @Test
    public void testLoadDataWithAnimals() {
        // Setup: create JSON with animals
        JSONObject data = new JSONObject();
        JSONArray animals = new JSONArray();

        JSONObject animal1 = new JSONObject();
        animal1.put("amount", 2);
        animal1.put("spec", new JSONObject().put("type", "sheep"));

        animals.put(animal1);
        data.put("animals", animals);

        // Execute: load data
        controller.loadData(data);

        // Verify: simulator.addAnimal was called correct number of times
        verify(simulator, times(2)).addAnimal(any(JSONObject.class));
    }

    @Test
    public void testLoadDataWithMultipleAnimalTypes() {
        // Setup: create JSON with multiple animal types
        JSONObject data = new JSONObject();
        JSONArray animals = new JSONArray();

        JSONObject sheep = new JSONObject();
        sheep.put("amount", 3);
        sheep.put("spec", new JSONObject().put("type", "sheep"));

        JSONObject wolves = new JSONObject();
        wolves.put("amount", 2);
        wolves.put("spec", new JSONObject().put("type", "wolf"));

        animals.put(sheep);
        animals.put(wolves);
        data.put("animals", animals);

        // Execute: load data
        controller.loadData(data);

        // Verify: simulator.addAnimal was called 5 times (3 + 2)
        verify(simulator, times(5)).addAnimal(any(JSONObject.class));
    }

    @Test
    public void testLoadDataWithRegions() {
        // Setup: create JSON with regions
        JSONObject data = new JSONObject();
        JSONArray regions = new JSONArray();

        JSONObject region = new JSONObject();
        region.put("spec", new JSONObject().put("type", "default"));
        region.put("row", new JSONArray().put(0).put(1)); // rows 0 to 1
        region.put("col", new JSONArray().put(0).put(2)); // cols 0 to 2

        regions.put(region);
        data.put("regions", regions);
        data.put("animals", new JSONArray()); // Empty animals array

        // Execute: load data
        controller.loadData(data);

        // Verify: simulator. was called for each region cell (2 rows * 3 cols = 6)
        verify(simulator, times(6)).setRegion(anyInt(), anyInt(), any(JSONObject.class));
    }

    @Test
    public void testLoadDataWithAnimalsAndRegions() {
        // Setup: create JSON with both animals and regions
        JSONObject data = new JSONObject();

        // Animals
        JSONArray animals = new JSONArray();
        JSONObject animal = new JSONObject();
        animal.put("amount", 1);
        animal.put("spec", new JSONObject().put("type", "sheep"));
        animals.put(animal);
        data.put("animals", animals);

        // Regions
        JSONArray regions = new JSONArray();
        JSONObject region = new JSONObject();
        region.put("spec", new JSONObject().put("type", "default"));
        region.put("row", new JSONArray().put(0).put(0)); // single row
        region.put("col", new JSONArray().put(0).put(0)); // single col
        regions.put(region);
        data.put("regions", regions);

        // Execute: load data
        controller.loadData(data);

        // Verify: both animals and regions were loaded
        verify(simulator, times(1)).addAnimal(any(JSONObject.class));
        verify(simulator, times(1)).setRegion(anyInt(), anyInt(), any(JSONObject.class));
    }

    @Test
    public void testLoadDataWithoutRegions() {
        // Setup: create JSON without regions field
        JSONObject data = new JSONObject();
        JSONArray animals = new JSONArray();

        JSONObject animal = new JSONObject();
        animal.put("amount", 1);
        animal.put("spec", new JSONObject().put("type", "sheep"));
        animals.put(animal);
        data.put("animals", animals);

        // Execute: load data (should not crash without regions)
        assertDoesNotThrow(() -> {
            controller.loadData(data);
        }, "Should handle data without regions field");

        // Verify: animals were loaded, no regions set
        verify(simulator, times(1)).addAnimal(any(JSONObject.class));
        verify(simulator, never()).setRegion(anyInt(), anyInt(), any(JSONObject.class));
    }

    @Test
    public void testLoadDataEmptyAnimals() {
        // Setup: create JSON with empty animals array
        JSONObject data = new JSONObject();
        data.put("animals", new JSONArray());

        // Execute: load data
        controller.loadData(data);

        // Verify: no animals added
        verify(simulator, never()).addAnimal(any(JSONObject.class));
    }

    // ========== RUN TESTS ==========

    @Test
    public void testRunWithoutViewer() {
        // Setup: configure simulator to stop after time
        when(simulator.getTime())
            .thenReturn(0.0, 1.0, 2.0, 3.0); // Progress time on each call

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // Execute: run simulation
        try {
          controller.run(2.5, 1.0, false, output);
          output.close();
        } catch (Exception e) {
          fail(e.getMessage());
        }

        // Verify: simulator.advance was called
        verify(simulator, atLeast(1)).advance(1.0);

        // Verify: output was generated
        assertTrue(output.size() > 0, "Should generate output");

        // Verify: output contains JSON
        String outputStr = output.toString();
        assertTrue(outputStr.contains("in"), "Output should contain 'in' field");
        assertTrue(outputStr.contains("out"), "Output should contain 'out' field");
    }

    @Test
    public void testRunWithViewer() {
        // Setup: configure simulator
        when(simulator.getTime())
            .thenReturn(0.0, 0.5, 1.0, 1.5); // Progress time

        // Note: getAnimals() already configured in setUp() to return empty list

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // Execute: run simulation with viewer
        assertDoesNotThrow(() -> {
            controller.run(1.0, 0.5, output);
        }, "Should run with viewer without errors");

        // Verify: simulator was advanced
        verify(simulator, atLeast(1)).advance(0.5);
    }

    @Test
    public void testRunWithNullOutputStream() {
        // Setup: configure simulator
        when(simulator.getTime())
            .thenReturn(0.0, 1.0, 2.0);

        // Execute: run with null output stream (should not crash)
        assertDoesNotThrow(() -> {
            controller.run(1.5, 1.0, null);
        }, "Should handle null output stream");

        // Verify: simulation ran
        verify(simulator, atLeast(1)).advance(1.0);
    }

    @Test
    public void testRunMultipleSteps() {
        // Setup: configure simulator to advance multiple times
        when(simulator.getTime())
            .thenReturn(0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // Execute: run for longer time with small dt
        try {
          controller.run(2.5, 0.5, output);
        } catch (Exception e) {
          fail(e.getMessage());
        }
        // Verify: advance was called multiple times
        verify(simulator, atLeast(4)).advance(0.5);
    }

    @Test
    public void testRunZeroTime() {
        // Setup: configure simulator
        when(simulator.getTime()).thenReturn(0.0);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // Execute: run with zero time
        try {
          controller.run(0.0, 1.0,  output);
        } catch (IOException e) {
          fail(e.getMessage());
        }

        // Verify: advance not called (already at target time)
        verify(simulator, never()).advance(anyDouble());

        // Verify: output still generated
        assertTrue(output.size() > 0, "Should generate output even with zero time");
    }

    @Test
    public void testRunOutputFormat() {
        // Setup: configure simulator to return specific JSON
        JSONObject initialState = new JSONObject();
        initialState.put("time", 0.0);
        initialState.put("state", new JSONObject());

        JSONObject finalState = new JSONObject();
        finalState.put("time", 2.0);
        finalState.put("state", new JSONObject());

        when(simulator.asJson())
            .thenReturn(initialState)  // Initial call
            .thenReturn(finalState);   // Final call

        when(simulator.getTime())
            .thenReturn(0.0, 1.0, 2.0, 3.0);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // Execute: run simulation
        try {
          controller.run(2.0, 1.0, output);
          output.close();
        } catch (IOException e) {
          fail(e.getMessage());
        }

        // Verify: output contains both initial and final states
        String outputStr = output.toString();
        JSONObject outputJson = new JSONObject(outputStr);

        assertTrue(outputJson.has("in"), "Output should have 'in' field");
        assertTrue(outputJson.has("out"), "Output should have 'out' field");
    }

    // ========== INTEGRATION TESTS ==========

    @Test
    public void testCompleteWorkflow() {
        // Setup: create data JSON
        JSONObject data = new JSONObject();

        // Add animals
        JSONArray animals = new JSONArray();
        JSONObject sheepSpec = new JSONObject();
        sheepSpec.put("amount", 2);
        sheepSpec.put("spec", new JSONObject().put("type", "sheep"));
        animals.put(sheepSpec);
        data.put("animals", animals);

        // Add regions
        JSONArray regions = new JSONArray();
        JSONObject regionSpec = new JSONObject();
        regionSpec.put("spec", new JSONObject().put("type", "default"));
        regionSpec.put("row", new JSONArray().put(0).put(1));
        regionSpec.put("col", new JSONArray().put(0).put(1));
        regions.put(regionSpec);
        data.put("regions", regions);

        // Configure simulator
        when(simulator.getTime())
            .thenReturn(0.0, 1.0, 2.0, 3.0);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // Execute: load data and run
        controller.loadData(data);
        try {
          controller.run(2.5, 1.0, output);
        } catch (IOException e) {
          fail(e.getMessage());
        }
        // Verify: animals were added
        verify(simulator, times(2)).addAnimal(any(JSONObject.class));

        // Verify: regions were set (2x2 grid = 4 cells)
        verify(simulator, times(4)).setRegion(anyInt(), anyInt(), any(JSONObject.class));

        // Verify: simulation ran
        verify(simulator, atLeast(1)).advance(anyDouble());

        // Verify: output generated
        assertTrue(output.size() > 0, "Should generate output");
    }

    @Test
    public void testLoadDataMultipleTimes() {
        // Setup: create two different data sets
        JSONObject data1 = new JSONObject();
        JSONArray animals1 = new JSONArray();
        JSONObject animal1 = new JSONObject();
        animal1.put("amount", 1);
        animal1.put("spec", new JSONObject().put("type", "sheep"));
        animals1.put(animal1);
        data1.put("animals", animals1);

        JSONObject data2 = new JSONObject();
        JSONArray animals2 = new JSONArray();
        JSONObject animal2 = new JSONObject();
        animal2.put("amount", 2);
        animal2.put("spec", new JSONObject().put("type", "wolf"));
        animals2.put(animal2);
        data2.put("animals", animals2);

        // Execute: load data multiple times
        controller.loadData(data1);
        controller.loadData(data2);

        // Verify: animals from both loads were added
        verify(simulator, times(3)).addAnimal(any(JSONObject.class)); // 1 + 2 = 3
    }

    @Test
    public void testRunAfterMultipleLoads() {
        // Setup: load animals
        JSONObject data = new JSONObject();
        JSONArray animals = new JSONArray();
        JSONObject animal = new JSONObject();
        animal.put("amount", 1);
        animal.put("spec", new JSONObject().put("type", "sheep"));
        animals.put(animal);
        data.put("animals", animals);

        controller.loadData(data);

        // Configure simulator for run
        when(simulator.getTime())
            .thenReturn(0.0, 0.5, 1.0, 1.5);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // Execute: run simulation
        try {
          controller.run(1.0, 0.5, output);
        } catch (IOException e) {
          fail(e.getMessage());
        }

        // Verify: everything worked together
        verify(simulator, times(1)).addAnimal(any(JSONObject.class));
        verify(simulator, atLeast(1)).advance(0.5);
        assertTrue(output.size() > 0, "Should generate output");
    }

    @Test
    public void testRegionRangeLoading() {
        // Setup: create region with specific range
        JSONObject data = new JSONObject();
        JSONArray regions = new JSONArray();

        JSONObject region = new JSONObject();
        region.put("spec", new JSONObject().put("type", "custom"));
        region.put("row", new JSONArray().put(0).put(2)); // rows 0, 1, 2
        region.put("col", new JSONArray().put(1).put(1)); // col 1 only

        regions.put(region);
        data.put("regions", regions);
        data.put("animals", new JSONArray());

        // Execute: load data
        controller.loadData(data);

        // Verify: setRegion called for each cell in range (3 rows * 1 col = 3)
        verify(simulator, times(3)).setRegion(anyInt(), anyInt(), any(JSONObject.class));

        // Verify specific calls using eq() for literal values
        verify(simulator).setRegion(eq(0), eq(1), any(JSONObject.class));
        verify(simulator).setRegion(eq(1), eq(1), any(JSONObject.class));
        verify(simulator).setRegion(eq(2), eq(1), any(JSONObject.class));
    }
}
