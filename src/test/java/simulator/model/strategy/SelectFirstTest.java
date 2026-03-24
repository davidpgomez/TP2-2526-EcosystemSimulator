package simulator.model.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import simulator.misc.Vector2D;
import simulator.model.animal.Animal;
import simulator.model.animal.AnimalTestHelper;
import simulator.model.animal.Sheep;
import simulator.model.animal.Wolf;
import simulator.model.region.AnimalMapView;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Test class for SelectFirst selection strategy
 */
public class SelectFirstTest {

    @Mock
    private AnimalMapView mockRegionManager;

    private SelectFirst selectFirst;
    private Animal referenceAnimal;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Configure region manager mock
        when(mockRegionManager.getWidth()).thenReturn(500);
        when(mockRegionManager.getHeight()).thenReturn(500);

        selectFirst = new SelectFirst();

        // Create a reference animal for selection
        referenceAnimal = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(250, 250));
        referenceAnimal.init(mockRegionManager);
    }

    @Test
    public void testSelectFromEmptyList() {
        // Setup: empty list
        List<Animal> animals = new ArrayList<>();

        // Execute: select from empty list
        Animal selected = selectFirst.select(referenceAnimal, animals);

        // Verify: should return null
        assertNull(selected, "SelectFirst should return null for empty list");
    }

    @Test
    public void testSelectFromSingleElementList() {
        // Setup: list with one animal
        Animal sheep = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100));
        sheep.init(mockRegionManager);

        List<Animal> animals = new ArrayList<>();
        animals.add(sheep);

        // Execute: select
        Animal selected = selectFirst.select(referenceAnimal, animals);

        // Verify: should return the only animal
        assertNotNull(selected, "SelectFirst should return an animal");
        assertEquals(sheep, selected, "SelectFirst should return the single animal in the list");
    }

    @Test
    public void testSelectReturnsFirstElement() {
        // Setup: list with multiple animals
        Animal sheep1 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100));
        sheep1.init(mockRegionManager);

        Animal sheep2 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(200, 200));
        sheep2.init(mockRegionManager);

        Animal sheep3 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(300, 300));
        sheep3.init(mockRegionManager);

        List<Animal> animals = new ArrayList<>();
        animals.add(sheep1);
        animals.add(sheep2);
        animals.add(sheep3);

        // Execute: select
        Animal selected = selectFirst.select(referenceAnimal, animals);

        // Verify: should return the first animal
        assertNotNull(selected, "SelectFirst should return an animal");
        assertEquals(sheep1, selected, "SelectFirst should return the first animal in the list");
    }

    @Test
    public void testSelectIgnoresAnimalProperties() {
        // Setup: list with animals of different properties (age, position, etc.)
        Animal youngSheep = AnimalTestHelper.createSheep(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100), 1.0);
        youngSheep.init(mockRegionManager);

        Animal oldSheep = AnimalTestHelper.createSheep(new SelectFirst(), new SelectFirst(), new Vector2D(400, 400), 7.0);
        oldSheep.init(mockRegionManager);

        Animal closeSheep = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(255, 255));
        closeSheep.init(mockRegionManager);

        // Add in specific order: old, close, young
        List<Animal> animals = new ArrayList<>();
        animals.add(oldSheep);
        animals.add(closeSheep);
        animals.add(youngSheep);

        // Execute: select
        Animal selected = selectFirst.select(referenceAnimal, animals);

        // Verify: should return the first animal regardless of properties
        assertEquals(oldSheep, selected,
            "SelectFirst should return first animal regardless of age or distance");
    }

    @Test
    public void testSelectWithDifferentSpecies() {
        // Setup: list with different animal species
        Animal wolf = new Wolf(new SelectFirst(), new SelectFirst(), new Vector2D(150, 150));
        wolf.init(mockRegionManager);

        Animal sheep = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(350, 350));
        sheep.init(mockRegionManager);

        List<Animal> animals = new ArrayList<>();
        animals.add(wolf);
        animals.add(sheep);

        // Execute: select
        Animal selected = selectFirst.select(referenceAnimal, animals);

        // Verify: should return the first animal (wolf)
        assertEquals(wolf, selected, "SelectFirst should return first animal regardless of species");
    }

    @Test
    public void testSelectIsConsistent() {
        // Setup: list with multiple animals
        Animal sheep1 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100));
        sheep1.init(mockRegionManager);

        Animal sheep2 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(200, 200));
        sheep2.init(mockRegionManager);

        List<Animal> animals = new ArrayList<>();
        animals.add(sheep1);
        animals.add(sheep2);

        // Execute: select multiple times
        Animal firstSelection = selectFirst.select(referenceAnimal, animals);
        Animal secondSelection = selectFirst.select(referenceAnimal, animals);
        Animal thirdSelection = selectFirst.select(referenceAnimal, animals);

        // Verify: should always return the same animal
        assertEquals(firstSelection, secondSelection,
            "SelectFirst should return consistent results");
        assertEquals(secondSelection, thirdSelection,
            "SelectFirst should return consistent results");
        assertEquals(sheep1, firstSelection,
            "SelectFirst should always return the first animal");
    }

    @Test
    public void testSelectWithNullReferenceAnimal() {
        // Setup: list with animals
        Animal sheep = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100));
        sheep.init(mockRegionManager);

        List<Animal> animals = new ArrayList<>();
        animals.add(sheep);

        // Execute: select with null reference animal
        Animal selected = selectFirst.select(null, animals);

        // Verify: should still return the first animal (reference animal not used)
        assertEquals(sheep, selected,
            "SelectFirst should work even with null reference animal");
    }
}
