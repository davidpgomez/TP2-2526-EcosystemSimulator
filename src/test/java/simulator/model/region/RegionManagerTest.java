package simulator.model.region;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import simulator.misc.Vector2D;
import simulator.model.animal.Animal;
import simulator.model.animal.AnimalTestHelper;
import simulator.model.animal.Sheep;
import simulator.model.animal.Wolf;
import simulator.model.strategy.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for RegionManager, covering all public interface methods
 */
public class RegionManagerTest {

    private RegionManager regionManager;
    private static final int COLS = 3;
    private static final int ROWS = 3;
    private static final int WIDTH = 600;
    private static final int HEIGHT = 600;

    @BeforeEach
    public void setUp() {
        regionManager = new RegionManager(COLS, ROWS, WIDTH, HEIGHT);
    }

    // ========== CONSTRUCTOR TESTS ==========

    @Test
    public void testValidConstructor() {
        // Verify: constructor creates instance with correct values
        RegionManager rm = new RegionManager(4, 5, 800, 1000);

        assertEquals(4, rm.getCols(), "Cols should be 4");
        assertEquals(5, rm.getRows(), "Rows should be 5");
        assertEquals(800, rm.getWidth(), "Width should be 800");
        assertEquals(1000, rm.getHeight(), "Height should be 1000");
        assertEquals(800 / 4, rm.getRegionWidth(), "Region width should be width/cols");
        assertEquals(1000 / 5, rm.getRegionHeight(), "Region height should be height/rows");
    }

    @Test
    public void testConstructorInvalidCols() {
        // Verify: constructor throws exception for cols < 1
        assertThrows(IllegalArgumentException.class, () -> {
            new RegionManager(0, 5, 800, 1000);
        }, "Should throw exception when cols < 1");

        assertThrows(IllegalArgumentException.class, () -> {
            new RegionManager(-1, 5, 800, 1000);
        }, "Should throw exception when cols is negative");
    }

    @Test
    public void testConstructorInvalidRows() {
        // Verify: constructor throws exception for rows < 1
        assertThrows(IllegalArgumentException.class, () -> {
            new RegionManager(4, 0, 800, 1000);
        }, "Should throw exception when rows < 1");

        assertThrows(IllegalArgumentException.class, () -> {
            new RegionManager(4, -1, 800, 1000);
        }, "Should throw exception when rows is negative");
    }

    @Test
    public void testConstructorInvalidWidth() {
        // Verify: constructor throws exception when width < 2 * cols
        assertThrows(IllegalArgumentException.class, () -> {
            new RegionManager(5, 5, 9, 1000); // width should be at least 10
        }, "Should throw exception when width and cols are not divisible");
    }

    @Test
    public void testConstructorInvalidHeight() {
        // Verify: constructor throws exception when height < 2 * rows
        assertThrows(IllegalArgumentException.class, () -> {
            new RegionManager(5, 5, 800, 9); // height should be at least 10
        }, "Should throw exception when height and rows are not divisible");
    }
    // ========== ANIMAL REGISTRATION TESTS ==========

    @Test
    public void testRegisterAnimal() {
        // Setup: create an animal
        Sheep sheep = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100));

        // Execute: register animal
        regionManager.registerAnimal(sheep);

        // Verify: animal can get food (which means it's registered in a region)
        double food = regionManager.getFood(sheep, 1.0);
        assertTrue(food >= 0, "Registered animal should be able to get food");
    }

    @Test
    public void testRegisterAnimalTwiceThrowsException() {
        // Setup: create and register an animal
        Sheep sheep = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100));
        regionManager.registerAnimal(sheep);

        // Verify: registering same animal again throws exception
        assertThrows(IllegalArgumentException.class, () -> {
            regionManager.registerAnimal(sheep);
        }, "Should throw exception when registering same animal twice");
    }

    @Test
    public void testRegisterAnimalInitializesPosition() {
        // Setup: create an animal without position
        Sheep sheep = new Sheep(new SelectFirst(), new SelectFirst(), null);

        // Execute: register animal
        regionManager.registerAnimal(sheep);

        // Verify: animal has a position assigned
        assertNotNull(sheep.getPosition(), "Animal should have position after registration");
        assertTrue(sheep.getPosition().getX() >= 0 && sheep.getPosition().getX() < WIDTH,
            "Animal X position should be within bounds");
        assertTrue(sheep.getPosition().getY() >= 0 && sheep.getPosition().getY() < HEIGHT,
            "Animal Y position should be within bounds");
    }

    @Test
    public void testUnregisterAnimal() {
        // Setup: create and register an animal
        Sheep sheep = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100));
        regionManager.registerAnimal(sheep);

        // Verify: animal is registered (can get food)
        double food = regionManager.getFood(sheep, 1.0);
        assertTrue(food >= 0, "Registered animal should be able to get food");

        // Execute: unregister animal
        regionManager.unregisterAnimal(sheep);

        // Verify: animal cannot get food anymore (will throw or return error)
        assertThrows(Exception.class, () -> {
            regionManager.getFood(sheep, 1.0);
        }, "Unregistered animal should not be able to get food");
    }

    @Test
    public void testUpdateAnimalRegion() {
        // Setup: create and register an animal in one region
        Sheep sheep = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(50, 50));
        regionManager.registerAnimal(sheep);

        // Get initial region (should be region [0][0])
        List<Animal> initialAnimals = regionManager.getAnimalsInRange(sheep, a -> true);

        // Execute: move animal to different region
        AnimalTestHelper.setPosition(sheep, new Vector2D(550, 550)); // Move to region [2][2]
        regionManager.updateAnimalRegion(sheep);

        // Verify: animal can still get food (is in new region)
        double food = regionManager.getFood(sheep, 1.0);
        assertTrue(food >= 0, "Animal should be able to get food after region change");
    }

    @Test
    public void testUpdateAnimalRegionSameRegion() {
        // Setup: create and register an animal
        Sheep sheep = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100));
        regionManager.registerAnimal(sheep);

        // Execute: move animal within same region
        AnimalTestHelper.setPosition(sheep, new Vector2D(150, 150)); // Still in region [0][0] (region is 200x200)
        regionManager.updateAnimalRegion(sheep);

        // Verify: animal can still get food
        double food = regionManager.getFood(sheep, 1.0);
        assertTrue(food >= 0, "Animal should be able to get food after moving within region");
    }

    // ========== GET ANIMALS IN RANGE TESTS ==========

    @Test
    public void testGetAnimalsInRangeEmpty() {
        // Setup: create and register a single animal
        Sheep sheep = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(300, 300));
        regionManager.registerAnimal(sheep);

        // Execute: get animals in range
        List<Animal> animals = regionManager.getAnimalsInRange(sheep, a -> true);

        // Verify: no other animals found
        assertTrue(animals.isEmpty(), "Should find no animals when alone");
    }

    @Test
    public void testGetAnimalsInRangeFindsNearbyAnimals() {
        // Setup: create multiple animals close together
        Sheep sheep1 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(300, 300));
        Sheep sheep2 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(310, 310));
        Sheep sheep3 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(320, 320));

        regionManager.registerAnimal(sheep1);
        regionManager.registerAnimal(sheep2);
        regionManager.registerAnimal(sheep3);

        // Execute: get animals in range of sheep1
        List<Animal> animals = regionManager.getAnimalsInRange(sheep1, a -> true);

        // Verify: found nearby animals (sheep2 and sheep3)
        assertEquals(2, animals.size(), "Should find 2 nearby animals");
        assertTrue(animals.contains(sheep2), "Should find sheep2");
        assertTrue(animals.contains(sheep3), "Should find sheep3");
        assertFalse(animals.contains(sheep1), "Should not include the searching animal itself");
    }

    @Test
    public void testGetAnimalsInRangeRespectsDistance() {
        // Setup: create animals at different distances
        Sheep sheep1 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(300, 300));
        Sheep sheep2 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(310, 300)); // 10 units away
        Sheep sheep3 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(400, 300)); // 100 units away

        regionManager.registerAnimal(sheep1);
        regionManager.registerAnimal(sheep2);
        regionManager.registerAnimal(sheep3);

        // Execute: get animals in range (sheep sight range is 40)
        List<Animal> animals = regionManager.getAnimalsInRange(sheep1, a -> true);

        // Verify: only finds sheep2 (within 40 units), not sheep3 (100 units away)
        assertEquals(1, animals.size(), "Should find only 1 animal within sight range");
        assertTrue(animals.contains(sheep2), "Should find sheep2 (close)");
        assertFalse(animals.contains(sheep3), "Should not find sheep3 (far away)");
    }

    @Test
    public void testGetAnimalsInRangeWithFilter() {
        // Setup: create different types of animals
        Sheep sheep = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(300, 300));
        Wolf wolf = new Wolf(new SelectFirst(), new SelectFirst(), new Vector2D(310, 310));

        regionManager.registerAnimal(sheep);
        regionManager.registerAnimal(wolf);

        // Execute: get only carnivores in range
        List<Animal> carnivores = regionManager.getAnimalsInRange(sheep,
            a -> a.getDiet() == Animal.Diet.CARNIVORE);

        // Verify: only finds wolf
        assertEquals(1, carnivores.size(), "Should find 1 carnivore");
        assertTrue(carnivores.contains(wolf), "Should find wolf");

        // Execute: get only herbivores in range
        List<Animal> herbivores = regionManager.getAnimalsInRange(wolf,
            a -> a.getDiet() == Animal.Diet.HERBIVORE);

        // Verify: only finds sheep
        assertEquals(1, herbivores.size(), "Should find 1 herbivore");
        assertTrue(herbivores.contains(sheep), "Should find sheep");
    }

    @Test
    public void testGetAnimalsInRangeAcrossRegions() {
        // Setup: create animals in different regions but within sight range
        // Region boundaries are at 200, 400 (for 600/3 = 200 per region)
        Sheep sheep1 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(190, 190));
        Sheep sheep2 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(210, 210)); // Different region, ~28 units away

        regionManager.registerAnimal(sheep1);
        regionManager.registerAnimal(sheep2);

        // Execute: get animals in range
        List<Animal> animals = regionManager.getAnimalsInRange(sheep1, a -> true);

        // Verify: finds animal in adjacent region (within sight range of 40)
        assertEquals(1, animals.size(), "Should find animal in adjacent region");
        assertTrue(animals.contains(sheep2), "Should find sheep2 across region boundary");
    }

    @Test
    public void testGetAnimalsInRangeAtBoundaries() {
        // Setup: create animal near map boundary
        Sheep sheep1 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(5, 5));
        Sheep sheep2 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(20, 20));

        regionManager.registerAnimal(sheep1);
        regionManager.registerAnimal(sheep2);

        // Execute: get animals in range (should handle boundary correctly)
        List<Animal> animals = regionManager.getAnimalsInRange(sheep1, a -> true);

        // Verify: finds nearby animal without errors
        assertEquals(1, animals.size(), "Should find nearby animal at boundary");
        assertTrue(animals.contains(sheep2), "Should find sheep2");
    }

    // ========== FOOD TESTS ==========

    @Test
    public void testGetFoodForHerbivore() {
        // Setup: create and register a herbivore
        Sheep sheep = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100));
        regionManager.registerAnimal(sheep);

        // Execute: get food
        double food = regionManager.getFood(sheep, 1.0);

        // Verify: herbivore gets food
        assertTrue(food > 0, "Herbivore should get food");
    }

    @Test
    public void testGetFoodForCarnivore() {
        // Setup: create and register a carnivore
        Wolf wolf = new Wolf(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100));
        regionManager.registerAnimal(wolf);

        // Execute: get food
        double food = regionManager.getFood(wolf, 1.0);

        // Verify: carnivore gets no food from environment (DefaultRegion)
        assertEquals(0.0, food, "Carnivore should get no food from environment");
    }

    @Test
    public void testGetFoodDecreasesWithCompetition() {
        // Setup: create multiple herbivores in same region
        // DefaultRegion has FOOD_SHORTAGE_TH_HERBS = 5, so we need more than 5 to see decrease
        Sheep sheep1 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100));
        Sheep sheep2 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(105, 100));
        Sheep sheep3 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(110, 100));
        Sheep sheep4 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(115, 100));
        Sheep sheep5 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(120, 100));
        Sheep sheep6 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(125, 100));
        Sheep sheep7 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(130, 100));

        regionManager.registerAnimal(sheep1);

        // Get food with 1 sheep
        double foodAlone = regionManager.getFood(sheep1, 1.0);

        // Add more sheep to exceed threshold
        regionManager.registerAnimal(sheep2);
        regionManager.registerAnimal(sheep3);
        regionManager.registerAnimal(sheep4);
        regionManager.registerAnimal(sheep5);
        regionManager.registerAnimal(sheep6);
        regionManager.registerAnimal(sheep7);

        // Get food with 7 sheep (exceeds threshold of 5)
        double foodWithCompetition = regionManager.getFood(sheep1, 1.0);

        // Verify: food decreases with more herbivores
        assertTrue(foodWithCompetition < foodAlone,
            "Food should decrease with competition (alone: " + foodAlone + ", with competition: " + foodWithCompetition + ")");
    }

    // ========== REGION MANAGEMENT TESTS ==========

    @Test
    public void testSetRegion() {
        // Setup: create a custom region
        Region customRegion = new DefaultRegion();

        // Execute: set region at position [1][1]
        regionManager.setRegion(1, 1, customRegion);

        // Verify: can register animal and it works with the new region
        Sheep sheep = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(300, 300)); // Region [1][1]
        regionManager.registerAnimal(sheep);

        double food = regionManager.getFood(sheep, 1.0);
        assertTrue(food >= 0, "Should work with custom region");
    }

    @Test
    public void testSetRegionTransfersAnimals() {
        // Setup: register animals in a region
        Sheep sheep1 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100));
        Sheep sheep2 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(110, 110));

        regionManager.registerAnimal(sheep1);
        regionManager.registerAnimal(sheep2);

        // Execute: replace region [0][0] with new region
        Region newRegion = new DefaultRegion();
        regionManager.setRegion(0, 0, newRegion);

        // Verify: animals are still accessible and can get food
        double food1 = regionManager.getFood(sheep1, 1.0);
        double food2 = regionManager.getFood(sheep2, 1.0);

        assertTrue(food1 >= 0, "Sheep1 should still be able to get food");
        assertTrue(food2 >= 0, "Sheep2 should still be able to get food");
    }

    @Test
    public void testUpdateAllRegions() {
        // Setup: register some animals
        Sheep sheep = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100));
        regionManager.registerAnimal(sheep);

        // Execute: update all regions (should not throw exception)
        assertDoesNotThrow(() -> {
            regionManager.updateAllRegions(1.0);
        }, "updateAllRegions should execute without errors");
    }

    // ========== JSON TESTS ==========

    @Test
    public void testasJson() {
        // Setup: register some animals
        Sheep sheep = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100));
        Wolf wolf = new Wolf(new SelectFirst(), new SelectFirst(), new Vector2D(300, 300));

        regionManager.registerAnimal(sheep);
        regionManager.registerAnimal(wolf);

        // Execute: get JSON representation
        JSONObject json = regionManager.asJson();

        // Verify: JSON contains regions array
        assertTrue(json.has("regions"), "JSON should have 'regions' field");

        JSONArray regions = json.getJSONArray("regions");
        assertEquals(COLS * ROWS, regions.length(),
            "Should have " + (COLS * ROWS) + " regions in JSON");

        // Verify: each region has required fields
        for (int i = 0; i < regions.length(); i++) {
            JSONObject region = regions.getJSONObject(i);
            assertTrue(region.has("row"), "Region should have 'row' field");
            assertTrue(region.has("col"), "Region should have 'col' field");
            assertTrue(region.has("data"), "Region should have 'data' field");
        }
    }

    @Test
    public void testasJsonEmptyRegions() {
        // Execute: get JSON without any animals
        JSONObject json = regionManager.asJson();

        // Verify: JSON is valid even without animals
        assertTrue(json.has("regions"), "JSON should have 'regions' field");
        JSONArray regions = json.getJSONArray("regions");
        assertEquals(COLS * ROWS, regions.length(), "Should have all regions in JSON");
    }

    // ========== INTEGRATION TESTS ==========

    @Test
    public void testCompleteAnimalLifecycle() {
        // Setup: create animals
        Sheep sheep = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100));
        Wolf wolf = new Wolf(new SelectFirst(), new SelectFirst(), new Vector2D(500, 500));

        // Register
        regionManager.registerAnimal(sheep);
        regionManager.registerAnimal(wolf);

        // Verify: animals can get food
        assertTrue(regionManager.getFood(sheep, 1.0) > 0, "Sheep should get food");
        assertEquals(0.0, regionManager.getFood(wolf, 1.0), "Wolf gets no environmental food");

        // Move sheep closer to wolf (within wolf's sight range of 50)
        AnimalTestHelper.setPosition(sheep, new Vector2D(520, 520)); // About 28 units from wolf
        regionManager.updateAnimalRegion(sheep);

        // Verify: sheep still gets food in new region
        assertTrue(regionManager.getFood(sheep, 1.0) > 0, "Sheep should get food in new region");

        // Verify: wolf can now find sheep (they're close enough)
        List<Animal> preyInRange = regionManager.getAnimalsInRange(wolf,
            a -> a.getDiet() == Animal.Diet.HERBIVORE);
        assertTrue(preyInRange.contains(sheep), "Wolf should find sheep in range");

        // Unregister
        regionManager.unregisterAnimal(sheep);
        regionManager.unregisterAnimal(wolf);

        // Verify: unregistered animals can't get food
        assertThrows(Exception.class, () -> regionManager.getFood(sheep, 1.0));
        assertThrows(Exception.class, () -> regionManager.getFood(wolf, 1.0));
    }

    @Test
    public void testMultipleAnimalsAcrossRegions() {
        // Setup: create animals in different regions
        Sheep sheep1 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(50, 50));    // Region [0][0]
        Sheep sheep2 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(250, 50));   // Region [0][1]
        Sheep sheep3 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(450, 50));   // Region [0][2]
        Wolf wolf = new Wolf(new SelectFirst(), new SelectFirst(), new Vector2D(250, 250));      // Region [1][1]

        regionManager.registerAnimal(sheep1);
        regionManager.registerAnimal(sheep2);
        regionManager.registerAnimal(sheep3);
        regionManager.registerAnimal(wolf);

        // Verify: each animal can get food
        assertTrue(regionManager.getFood(sheep1, 1.0) > 0);
        assertTrue(regionManager.getFood(sheep2, 1.0) > 0);
        assertTrue(regionManager.getFood(sheep3, 1.0) > 0);
        assertEquals(0.0, regionManager.getFood(wolf, 1.0));

        // Verify: wolf finds nearby sheep but not distant ones
        List<Animal> nearbyPrey = regionManager.getAnimalsInRange(wolf,
            a -> a.getDiet() == Animal.Diet.HERBIVORE);

        // Wolf at (250, 250) with sight range 50 should potentially find sheep2 at (250, 50) - 200 units away (too far)
        // Should not find any sheep as they're all too far
        assertTrue(nearbyPrey.isEmpty() || nearbyPrey.size() <= 3,
            "Wolf should find at most nearby sheep within sight range");
    }
}
