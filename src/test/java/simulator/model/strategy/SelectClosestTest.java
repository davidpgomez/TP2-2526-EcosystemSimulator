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
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Test class for SelectClosest selection strategy
 */
public class SelectClosestTest {

    @Mock
    private AnimalMapView mockRegionManager;

    private SelectClosest selectClosest;
    private Animal referenceAnimal;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Configure region manager mock
        when(mockRegionManager.getWidth()).thenReturn(500);
        when(mockRegionManager.getHeight()).thenReturn(500);

        selectClosest = new SelectClosest();

        // Create a reference animal for selection at position (250, 250)
        referenceAnimal = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(250, 250));
        referenceAnimal.init(mockRegionManager);
        AnimalTestHelper.setPosition(referenceAnimal, new Vector2D(250, 250));
    }

    @Test
    public void testSelectFromEmptyList() {
        // Setup: empty list
        List<Animal> animals = new ArrayList<>();

        // Execute & Verify: should throw NoSuchElementException
        assertThrows(NoSuchElementException.class, () -> {
            selectClosest.select(referenceAnimal, animals);
        }, "SelectClosest should throw NoSuchElementException for empty list");
    }

    @Test
    public void testSelectFromSingleElementList() {
        // Setup: list with one animal
        Animal sheep = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100));
        sheep.init(mockRegionManager);
        AnimalTestHelper.setPosition(sheep, new Vector2D(100, 100));

        List<Animal> animals = new ArrayList<>();
        animals.add(sheep);

        // Execute: select
        Animal selected = selectClosest.select(referenceAnimal, animals);

        // Verify: Due to bug, returns reference animal
        assertNotNull(selected, "SelectClosest should return an animal");
        assertEquals(sheep, selected, "The animal returned should be the only one in list");
    }

    @Test
    public void testSelectClosestAnimal() {
        // Setup: reference animal at (250, 250)
        AnimalTestHelper.setPosition(referenceAnimal, new Vector2D(250, 250));
        // Create animals at different distances
        Animal closeSheep = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(255, 255));
        closeSheep.init(mockRegionManager);
        AnimalTestHelper.setPosition(closeSheep, new Vector2D(255, 255));

        Animal mediumSheep = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(300, 300));
        mediumSheep.init(mockRegionManager);
        AnimalTestHelper.setPosition(mediumSheep, new Vector2D(300, 300));

        Animal farSheep = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(450, 450));
        farSheep.init(mockRegionManager);
        AnimalTestHelper.setPosition(farSheep, new Vector2D(450, 450));

        // Add in random order
        List<Animal> animals = new ArrayList<>();
        animals.add(farSheep);
        animals.add(closeSheep);
        animals.add(mediumSheep);

        // Execute: select
        Animal selected = selectClosest.select(referenceAnimal, animals);

        // Verify: Due to bug, returns reference animal instead of closest
        assertNotNull(selected, "SelectClosest should return an animal");
        assertEquals(closeSheep, selected, "SelectClosest should return the closest animal");
    }

    @Test
    public void testSelectWithSameDistance() {
        // Setup: animals at the same distance from
        // reference at (250, 250)
        AnimalTestHelper.setPosition(referenceAnimal, new Vector2D(250, 250));
        Animal sheep1 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(260, 250));
        sheep1.init(mockRegionManager);
        AnimalTestHelper.setPosition(sheep1, new Vector2D(260, 250));

        Animal sheep2 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(240, 250));
        sheep2.init(mockRegionManager);
        AnimalTestHelper.setPosition(sheep2, new Vector2D(240, 250));

        Animal sheep3 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(250, 260));
        sheep3.init(mockRegionManager);
        AnimalTestHelper.setPosition(sheep3, new Vector2D(250, 260));

        List<Animal> animals = new ArrayList<>();
        animals.add(sheep1);
        animals.add(sheep2);
        animals.add(sheep3);

        // Execute: select
        Animal selected = selectClosest.select(referenceAnimal, animals);

        // Verify: Due to bug, returns reference animal
        assertNotNull(selected, "SelectClosest should return an animal");
        assertEquals(sheep1, selected, "SelectClosest should return the first sheep found at minimum distance");
    }

    @Test
    public void testSelectClosestWithDifferentSpecies() {
        // Setup: different species at different distances
        AnimalTestHelper.setPosition(referenceAnimal, new Vector2D(250, 250));
        Animal closeWolf = new Wolf(new SelectFirst(), new SelectFirst(), new Vector2D(260, 260));
        closeWolf.init(mockRegionManager);
        AnimalTestHelper.setPosition(closeWolf, new Vector2D(260, 260));

        Animal farSheep = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(400, 400));
        farSheep.init(mockRegionManager);
        AnimalTestHelper.setPosition(farSheep, new Vector2D(400, 400));

        List<Animal> animals = new ArrayList<>();
        animals.add(farSheep);
        animals.add(closeWolf);

        // Execute: select
        Animal selected = selectClosest.select(referenceAnimal, animals);

        // Verify: should return the closest animal (wolf)
        assertEquals(closeWolf, selected,
                "SelectClosest should return the closest animal regardless of species");
    }

    @Test
    public void testSelectAtSamePosition() {
        // Setup: animal at the exact same position as reference
        AnimalTestHelper.setPosition(referenceAnimal, new Vector2D(250, 250));
        Animal samePosAnimal = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(250, 250));
        samePosAnimal.init(mockRegionManager);
        AnimalTestHelper.setPosition(samePosAnimal, new Vector2D(250, 250));

        Animal nearbyAnimal = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(251, 251));
        nearbyAnimal.init(mockRegionManager);
        AnimalTestHelper.setPosition(nearbyAnimal, new Vector2D(251, 251));

        List<Animal> animals = new ArrayList<>();
        animals.add(nearbyAnimal);
        animals.add(samePosAnimal);

        // Execute: select
        Animal selected = selectClosest.select(referenceAnimal, animals);

        // Verify: should return the animal at the same position (distance 0)
        assertEquals(samePosAnimal, selected,
                "SelectClosest should return animal at same position (distance = 0)");
    }

    @Test
    public void testSelectWithVerySmallDistanceDifferences() {
        // Setup: animals with very small distance differences
        AnimalTestHelper.setPosition(referenceAnimal, new Vector2D(250, 250));
        Animal sheep1 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(251.0, 250.0));
        sheep1.init(mockRegionManager);
        AnimalTestHelper.setPosition(sheep1, new Vector2D(251.0, 250.0));

        Animal sheep2 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(250.9, 250.0));
        sheep2.init(mockRegionManager);
        AnimalTestHelper.setPosition(sheep2, new Vector2D(250.9, 250.0));

        Animal sheep3 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(251.1, 250.0));
        sheep3.init(mockRegionManager);
        AnimalTestHelper.setPosition(sheep3, new Vector2D(251.1, 250.0));

        List<Animal> animals = new ArrayList<>();
        animals.add(sheep1);
        animals.add(sheep3);
        animals.add(sheep2);

        // Execute: select
        Animal selected = selectClosest.select(referenceAnimal, animals);

        // Verify: should return the closest animal (sheep2 at distance 0.9)
        assertEquals(sheep2, selected, "SelectClosest should return the closest animal");
    }

    @Test
    public void testSelectDiagonalDistance() {
        // Setup: test diagonal distances
        AnimalTestHelper.setPosition(referenceAnimal, new Vector2D(250, 250));
        Animal horizontal = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(270, 250));
        horizontal.init(mockRegionManager);
        AnimalTestHelper.setPosition(horizontal, new Vector2D(270, 250));

        Animal vertical = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(250, 270));
        vertical.init(mockRegionManager);
        AnimalTestHelper.setPosition(vertical, new Vector2D(250, 270));

        Animal diagonal = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(265, 265));
        diagonal.init(mockRegionManager);
        AnimalTestHelper.setPosition(diagonal, new Vector2D(265, 265));

        List<Animal> animals = new ArrayList<>();
        animals.add(horizontal);
        animals.add(vertical);
        animals.add(diagonal);

        // Execute: select
        Animal selected = selectClosest.select(referenceAnimal, animals);

        // Verify: should return one of the animals at minimum distance (horizontal or vertical, both at distance 20)
        assertTrue(selected == horizontal || selected == vertical,
                "SelectClosest should return one of the equally distant animals (horizontal or vertical)");
    }

    @Test
    public void testSelectIsConsistent() {
        // Setup: list with animals at different distances
        AnimalTestHelper.setPosition(referenceAnimal, new Vector2D(250, 250));
        Animal close = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(260, 260));
        close.init(mockRegionManager);
        AnimalTestHelper.setPosition(close, new Vector2D(260, 260));

        Animal far = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(400, 400));
        far.init(mockRegionManager);
        AnimalTestHelper.setPosition(far, new Vector2D(400, 400));

        List<Animal> animals = new ArrayList<>();
        animals.add(far);
        animals.add(close);

        // Execute: select multiple times
        Animal firstSelection = selectClosest.select(referenceAnimal, animals);
        Animal secondSelection = selectClosest.select(referenceAnimal, animals);
        Animal thirdSelection = selectClosest.select(referenceAnimal, animals);

        // Verify: should always return the same animal (the closest one)
        assertEquals(firstSelection, secondSelection,
                "SelectClosest should return consistent results");
        assertEquals(secondSelection, thirdSelection,
                "SelectClosest should return consistent results");
        assertEquals(close, firstSelection,
                "SelectClosest should return the closest animal");
    }

    @Test
    public void testSelectChangesWithReferencePosition() {
        // Setup: animals at fixed positions
        Animal sheep1 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100));
        sheep1.init(mockRegionManager);
        AnimalTestHelper.setPosition(sheep1, new Vector2D(100, 100));

        Animal sheep2 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(400, 400));
        sheep2.init(mockRegionManager);
        AnimalTestHelper.setPosition(sheep2, new Vector2D(400, 400));

        List<Animal> animals = new ArrayList<>();
        animals.add(sheep1);
        animals.add(sheep2);

        // Execute: select with reference at (250, 250)
        AnimalTestHelper.setPosition(referenceAnimal, new Vector2D(250, 250));
        Animal selectedFromCenter = selectClosest.select(referenceAnimal, animals);

        // Execute: select with reference at (50, 50)
        Animal anotherReferenceAnimal = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(50, 50));
        anotherReferenceAnimal.init(mockRegionManager);
        AnimalTestHelper.setPosition(anotherReferenceAnimal, new Vector2D(50, 50));
        Animal selectedFromCorner = selectClosest.select(anotherReferenceAnimal, animals);

        // Verify: closest changes based on reference position
        assertEquals(sheep1, selectedFromCenter,
                "From (250,250), sheep1 at (100,100) and sheep2 at (400,400) are equidistant, should return sheep1");
        assertEquals(sheep1, selectedFromCorner,
                "From (50,50), sheep1 at (100,100) is closer than sheep2 at (400,400)");

        // Move reference to (450, 450)
        Animal anotherReferenceAnimal2 = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(450, 450));
        anotherReferenceAnimal2.init(mockRegionManager);
        AnimalTestHelper.setPosition(anotherReferenceAnimal2, new Vector2D(450, 450));
        Animal selectedFromFarCorner = selectClosest.select(anotherReferenceAnimal2, animals);

        assertEquals(sheep2, selectedFromFarCorner,
                "From (450,450), sheep2 at (400,400) is closer than sheep1 at (100,100)");
    }
}
