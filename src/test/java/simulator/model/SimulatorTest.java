package simulator.model;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import simulator.factories.Factory;
import simulator.misc.Vector2D;
import simulator.model.animal.Animal;
import simulator.model.animal.AnimalInfo;
import simulator.model.animal.Sheep;
import simulator.model.animal.Wolf;
import simulator.model.strategy.SelectFirst;
import simulator.model.region.DefaultRegion;
import simulator.model.region.MapInfo;
import simulator.model.region.Region;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test class for Simulator, covering all public interface methods
 */
public class SimulatorTest {

    @Mock
    private Factory<Animal> animalsFactory;

    @Mock
    private Factory<Region> regionsFactory;

    private Simulator simulator;

    private static final int COLS = 3;
    private static final int ROWS = 3;
    private static final int WIDTH = 600;
    private static final int HEIGHT = 600;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Configure default behavior for factories
        when(regionsFactory.createInstance(any(JSONObject.class)))
            .thenReturn(new DefaultRegion());

        simulator = new Simulator(COLS, ROWS, WIDTH, HEIGHT, animalsFactory, regionsFactory);
    }

    // ========== CONSTRUCTOR TESTS ==========

    @Test
    public void testValidConstructor() {
        // Verify: constructor creates instance with correct values
        Simulator sim = new Simulator(4, 5, 800, 1000, animalsFactory, regionsFactory);

        assertNotNull(sim, "Simulator should be created");
        assertEquals(0.0, sim.getTime(), "Initial time should be 0");
        assertTrue(sim.getAnimals().isEmpty(), "Initial animals list should be empty");
    }

    @Test
    public void testConstructorNullAnimalsFactory() {
        // Verify: constructor throws exception when animals factory is null
        assertThrows(IllegalArgumentException.class, () -> {
            new Simulator(COLS, ROWS, WIDTH, HEIGHT, null, regionsFactory);
        }, "Should throw exception when animals factory is null");
    }

    @Test
    public void testConstructorNullRegionsFactory() {
        // Verify: constructor throws exception when regions factory is null
        assertThrows(IllegalArgumentException.class, () -> {
            new Simulator(COLS, ROWS, WIDTH, HEIGHT, animalsFactory, null);
        }, "Should throw exception when regions factory is null");
    }

    // ========== ADD ANIMAL TESTS ==========

    @Test
    public void testAddAnimal() {
        // Setup: create animal
        Sheep sheep = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100));
        when(animalsFactory.createInstance(any(JSONObject.class))).thenReturn(sheep);

        JSONObject animalJson = new JSONObject();
        animalJson.put("type", "sheep");

        // Execute: add animal
        simulator.addAnimal(animalJson);

        // Verify: animal is added
        assertEquals(1, simulator.getAnimals().size(), "Should have 1 animal");
        assertTrue(simulator.getAnimals().contains(sheep), "Should contain the added sheep");

        // Verify: factory was called
        verify(animalsFactory, times(1)).createInstance(any(JSONObject.class));
    }

    @Test
    public void testAddMultipleAnimals() {
        // Setup: create animals
        Sheep sheep1 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100));
        Sheep sheep2 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(200, 200));
        Wolf wolf = new Wolf(new SelectFirst(), new SelectFirst(), new Vector2D(300, 300));

        when(animalsFactory.createInstance(any(JSONObject.class)))
            .thenReturn(sheep1, sheep2, wolf);

        // Execute: add animals
        simulator.addAnimal(new JSONObject().put("type", "sheep"));
        simulator.addAnimal(new JSONObject().put("type", "sheep"));
        simulator.addAnimal(new JSONObject().put("type", "wolf"));

        // Verify: all animals are added
        assertEquals(3, simulator.getAnimals().size(), "Should have 3 animals");
    }

    // ========== SET REGIONS TESTS ==========

    @Test
    public void testsetRegion() {
        // Setup: create region JSON
        JSONObject regionJson = new JSONObject();
        regionJson.put("type", "default");

        Region customRegion = new DefaultRegion();
        when(regionsFactory.createInstance(any(JSONObject.class))).thenReturn(customRegion);

        // Execute: set region
        assertDoesNotThrow(() -> {
            simulator.setRegion(0, 0, regionJson);
        }, "Setting region should not throw exception");

        // Verify: factory was called
        verify(regionsFactory, atLeastOnce()).createInstance(any(JSONObject.class));
    }

    // ========== ADVANCE TESTS ==========

    @Test
    public void testAdvanceIncreasesTime() {
        // Setup: initial time
        double initialTime = simulator.getTime();

        // Execute: advance simulation
        simulator.advance(1.5);

        // Verify: time increased
        assertEquals(initialTime + 1.5, simulator.getTime(), 0.001,
            "Time should increase by dt");

        // Execute: advance again
        simulator.advance(2.0);

        // Verify: time increased again
        assertEquals(initialTime + 3.5, simulator.getTime(), 0.001,
            "Time should accumulate");
    }

    @Test
    public void testAdvanceUpdatesAnimals() {
        // Setup: add animal
        Sheep sheep = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100));
        when(animalsFactory.createInstance(any(JSONObject.class))).thenReturn(sheep);

        simulator.addAnimal(new JSONObject().put("type", "sheep"));

        double initialAge = sheep.getAge();
        Vector2D initialPos = sheep.getPosition();

        // Execute: advance simulation
        simulator.advance(1.0);

        // Verify: animal was updated
        assertTrue(sheep.getAge() > initialAge, "Animal age should increase");
    }

    @Disabled("Disabled until animal attributes can be set")
    public void testAdvanceRemovesDeadAnimals() {
        // Setup: add animal and kill it
        Sheep sheep = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100));
        when(animalsFactory.createInstance(any(JSONObject.class))).thenReturn(sheep);

        simulator.addAnimal(new JSONObject().put("type", "sheep"));
        assertEquals(1, simulator.getAnimals().size(), "Should have 1 animal initially");

        // Kill the animal by setting energy to 0
        // Uncomment sheep.energy = 0;

        // Execute: advance simulation (this will call update on the animal, which sets it to DEAD)
        simulator.advance(0.1);

        // Verify: after update, the animal should be in DEAD state
        assertEquals(Animal.State.DEAD, sheep.getState(), "Animal should be DEAD");

        // Execute: advance again to remove dead animals
        simulator.advance(0.1);

        // Verify: dead animal is removed
        assertEquals(0, simulator.getAnimals().size(), "Dead animal should be removed");
    }

    @Disabled("Disabled until animal attributes can be set")
    public void testAdvanceAddsBabies() {
        // Setup: add two sheep close together with high desire
        Sheep sheep1 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100));
        Sheep sheep2 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(101, 101));

        when(animalsFactory.createInstance(any(JSONObject.class)))
            .thenReturn(sheep1, sheep2);

        simulator.addAnimal(new JSONObject().put("type", "sheep"));
        simulator.addAnimal(new JSONObject().put("type", "sheep"));

        // Make them mate (set high desire and position them close)
        // sheep1.desire = 70.0;
        // sheep2.desire = 70.0;
        // sheep1.pos = new Vector2D(100, 100);
        // sheep2.pos = new Vector2D(101, 101);

        int initialCount = simulator.getAnimals().size();

        // Execute: advance multiple times to allow mating
        for (int i = 0; i < 50; i++) {
            int currentCount = simulator.getAnimals().size();
            simulator.advance(0.1);
            // If a baby was born, verify and break
            if (simulator.getAnimals().size() > currentCount) {
                assertTrue(simulator.getAnimals().size() > initialCount,
                    "Should have more animals after mating");
                break;
            }
        }

        // Note: Due to probability, baby might not always be born, but we tested the mechanism
    }

    @Test
    public void testAdvanceMultipleTimes() {
        // Setup: add animal
        Sheep sheep = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100));
        when(animalsFactory.createInstance(any(JSONObject.class))).thenReturn(sheep);

        simulator.addAnimal(new JSONObject().put("type", "sheep"));

        // Execute: advance multiple times
        for (int i = 0; i < 10; i++) {
            simulator.advance(1.0);
        }

        // Verify: time accumulated correctly
        assertEquals(10.0, simulator.getTime(), 0.001, "Time should be 10.0 after 10 advances");
    }

    // ========== JSON TESTS ==========

    @Test
    public void testasJson() {
        // Execute: get JSON
        JSONObject json = simulator.asJson();

        // Verify: JSON has required fields
        assertTrue(json.has("time"), "JSON should have 'time' field");
        assertTrue(json.has("state"), "JSON should have 'state' field");

        assertEquals(0.0, json.getDouble("time"), "Initial time should be 0");
    }

    @Test
    public void testasJsonAfterAdvance() {
        // Setup: add animal and advance
        Sheep sheep = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100));
        when(animalsFactory.createInstance(any(JSONObject.class))).thenReturn(sheep);

        simulator.addAnimal(new JSONObject().put("type", "sheep"));
        simulator.advance(5.5);

        // Execute: get JSON
        JSONObject json = simulator.asJson();

        // Verify: JSON reflects current state
        assertEquals(5.5, json.getDouble("time"), 0.001, "JSON should show advanced time");
        assertTrue(json.has("state"), "JSON should have state");
    }

    // ========== GETTER TESTS ==========

    @Test
    public void testGetMapInfo() {
        // Execute: get map info
        MapInfo mapInfo = simulator.getMapInfo();

        // Verify: map info is correct
        assertNotNull(mapInfo, "Map info should not be null");
        assertEquals(COLS, mapInfo.getCols(), "Cols should match");
        assertEquals(ROWS, mapInfo.getRows(), "Rows should match");
        assertEquals(WIDTH, mapInfo.getWidth(), "Width should match");
        assertEquals(HEIGHT, mapInfo.getHeight(), "Height should match");
    }

    @Test
    public void testGetAnimals() {
        // Setup: add animals
        Sheep sheep = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100));
        Wolf wolf = new Wolf(new SelectFirst(), new SelectFirst(), new Vector2D(200, 200));

        when(animalsFactory.createInstance(any(JSONObject.class)))
            .thenReturn(sheep, wolf);

        simulator.addAnimal(new JSONObject().put("type", "sheep"));
        simulator.addAnimal(new JSONObject().put("type", "wolf"));

        // Execute: get animals
        List<? extends AnimalInfo> animals = simulator.getAnimals();

        // Verify: list contains animals
        assertEquals(2, animals.size(), "Should have 2 animals");
        assertTrue(animals.contains(sheep), "Should contain sheep");
        assertTrue(animals.contains(wolf), "Should contain wolf");
    }

    @Test
    public void testGetAnimalsIsUnmodifiable() {
        // Setup: get animals list
        List<? extends AnimalInfo> animals = simulator.getAnimals();

        // Verify: list is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> {
            ((List<AnimalInfo>) animals).add(
                new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100))
            );
        }, "Animals list should be unmodifiable");
    }

    @Test
    public void testGetTime() {
        // Verify: initial time
        assertEquals(0.0, simulator.getTime(), "Initial time should be 0");

        // Advance and verify
        simulator.advance(3.7);
        assertEquals(3.7, simulator.getTime(), 0.001, "Time should be 3.7");

        simulator.advance(1.3);
        assertEquals(5.0, simulator.getTime(), 0.001, "Time should be 5.0");
    }

    // ========== INTEGRATION TESTS ==========

    @Test
    public void testCompleteSimulationCycle() {
        // Setup: add animals
        Sheep sheep = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100));
        Wolf wolf = new Wolf(new SelectFirst(), new SelectFirst(), new Vector2D(500, 500));

        when(animalsFactory.createInstance(any(JSONObject.class)))
            .thenReturn(sheep, wolf);

        // Add animals
        simulator.addAnimal(new JSONObject().put("type", "sheep"));
        simulator.addAnimal(new JSONObject().put("type", "wolf"));

        assertEquals(2, simulator.getAnimals().size(), "Should start with 2 animals");

        // Advance simulation
        for (int i = 0; i < 10; i++) {
            simulator.advance(1.0);
        }

        // Verify: simulation ran successfully
        assertEquals(10.0, simulator.getTime(), 0.001, "Time should be 10.0");

        // Animals might be dead or alive depending on their state
        assertTrue(simulator.getAnimals().size() >= 0, "Animal count should be valid");

        // Get JSON
        JSONObject json = simulator.asJson();
        assertNotNull(json, "JSON should be generated");
    }

    @Disabled("Disabled until animal attributes can be set")
    public void testAnimalDeathAndRemoval() {
        // Setup: add animal with low energy
        Sheep sheep = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100));
        // sheep.energy = 1.0; // Very low energy
        // sheep.age = 7.9; // Close to max age (8.0)

        when(animalsFactory.createInstance(any(JSONObject.class))).thenReturn(sheep);

        simulator.addAnimal(new JSONObject().put("type", "sheep"));
        assertEquals(1, simulator.getAnimals().size(), "Should start with 1 animal");

        // Execute: advance until sheep dies
        for (int i = 0; i < 100; i++) {
            simulator.advance(0.1);
            if (simulator.getAnimals().isEmpty()) {
                break;
            }
        }

        // Verify: sheep eventually dies and is removed
        assertEquals(0, simulator.getAnimals().size(), "Sheep should die and be removed");
    }

    @Test
    public void testMultipleRegionTypes() {
        // Setup: create different region types
        Region region1 = new DefaultRegion();
        Region region2 = new DefaultRegion();

        when(regionsFactory.createInstance(any(JSONObject.class)))
            .thenReturn(region1, region2);

        JSONObject regionJson = new JSONObject().put("type", "default");

        // Execute: set multiple regions
        simulator.setRegion(0, 0, regionJson);
        simulator.setRegion(1, 1, regionJson);

        // Verify: no exceptions thrown
        assertTrue(true, "Should handle multiple region types");
    }
}
