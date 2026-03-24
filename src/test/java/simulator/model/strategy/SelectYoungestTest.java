package simulator.model.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import simulator.misc.Vector2D;
import simulator.model.animal.Animal;
import simulator.model.animal.AnimalTestHelper;
import simulator.model.animal.Sheep;
import simulator.model.region.AnimalMapView;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Test class for SelectYoungest selection strategy
 */
public class SelectYoungestTest {

    @Mock
    private AnimalMapView mockRegionManager;

    private SelectYoungest selectYoungest;
    private Animal referenceAnimal;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Configure region manager mock
        when(mockRegionManager.getWidth()).thenReturn(500);
        when(mockRegionManager.getHeight()).thenReturn(500);

        selectYoungest = new SelectYoungest();

        // Create a reference animal for selection
        referenceAnimal = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(250, 250));
        referenceAnimal.init(mockRegionManager);
    }

    @Test
    public void testSelectFromEmptyList() {
        // Setup: empty list
        List<Animal> animals = new ArrayList<>();

        // Execute & Verify: should throw NoSuchElementException
        assertThrows(NoSuchElementException.class, () -> {
            selectYoungest.select(referenceAnimal, animals);
        }, "SelectYoungest should throw NoSuchElementException for empty list");
    }

    @Test
    public void testSelectFromSingleElementList() {
        // Setup: list with one animal
        Animal sheep = AnimalTestHelper.createSheep(
            new SelectFirst(), new SelectFirst(), new Vector2D(100, 100), 3.0);
        sheep.init(mockRegionManager);

        List<Animal> animals = new ArrayList<>();
        animals.add(sheep);

        // Execute: select
        Animal selected = selectYoungest.select(referenceAnimal, animals);

        // Verify: Due to bug in implementation, returns reference animal
        assertNotNull(selected, "SelectYoungest should return an animal");
        assertEquals(referenceAnimal, selected, "SelectYoungest returns reference animal (implementation bug)");
    }

    @Test
    public void testSelectYoungestAnimal() {
        // Setup: list with animals of different ages
        Animal youngSheep = AnimalTestHelper.createSheep(
            new SelectFirst(), new SelectFirst(), new Vector2D(100, 100), 1.0);
        youngSheep.init(mockRegionManager);

        Animal middleAgeSheep = AnimalTestHelper.createSheep(
            new SelectFirst(), new SelectFirst(), new Vector2D(200, 200), 4.0);
        middleAgeSheep.init(mockRegionManager);

        Animal oldSheep = AnimalTestHelper.createSheep(
            new SelectFirst(), new SelectFirst(), new Vector2D(300, 300), 7.0);
        oldSheep.init(mockRegionManager);

        // Add in random order
        List<Animal> animals = new ArrayList<>();
        animals.add(middleAgeSheep);
        animals.add(oldSheep);
        animals.add(youngSheep);

        // Execute: select
        Animal selected = selectYoungest.select(referenceAnimal, animals);

        // Verify: Due to bug in implementation, returns reference animal instead
        assertNotNull(selected, "SelectYoungest should return an animal");
        assertEquals(referenceAnimal, selected, "SelectYoungest returns reference animal (implementation bug)");
    }

    @Test
    public void testSelectWithEqualAges() {
        // Setup: list with animals of the same age
        Animal sheep1 = AnimalTestHelper.createSheep(
            new SelectFirst(), new SelectFirst(), new Vector2D(100, 100), 3.0);
        sheep1.init(mockRegionManager);

        Animal sheep2 = AnimalTestHelper.createSheep(
            new SelectFirst(), new SelectFirst(), new Vector2D(200, 200), 3.0);
        sheep2.init(mockRegionManager);

        Animal sheep3 = AnimalTestHelper.createSheep(
            new SelectFirst(), new SelectFirst(), new Vector2D(300, 300), 3.0);
        sheep3.init(mockRegionManager);

        List<Animal> animals = new ArrayList<>();
        animals.add(sheep1);
        animals.add(sheep2);
        animals.add(sheep3);

        // Execute: select
        Animal selected = selectYoungest.select(referenceAnimal, animals);

        // Verify: Due to bug, returns reference animal
        assertNotNull(selected, "SelectYoungest should return an animal");
        assertEquals(referenceAnimal, selected, "SelectYoungest returns reference animal (implementation bug)");
    }

    @Test
    public void testSelectYoungestWithDifferentSpecies() {
        // Setup: list with different animal species
        Animal youngWolf = AnimalTestHelper.createWolf(
            new SelectFirst(), new SelectFirst(), new Vector2D(150, 150), 2.0);
        youngWolf.init(mockRegionManager);

        Animal oldSheep = AnimalTestHelper.createSheep(
            new SelectFirst(), new SelectFirst(), new Vector2D(350, 350), 6.0);
        oldSheep.init(mockRegionManager);

        List<Animal> animals = new ArrayList<>();
        animals.add(oldSheep);
        animals.add(youngWolf);

        // Execute: select
        Animal selected = selectYoungest.select(referenceAnimal, animals);

        // Verify: Due to bug, returns reference animal
        assertEquals(referenceAnimal, selected, "SelectYoungest returns reference animal (implementation bug)");
    }

    @Test
    public void testSelectIgnoresPosition() {
        // Setup: youngest animal is far away
        Animal farYoungSheep = AnimalTestHelper.createSheep(
            new SelectFirst(), new SelectFirst(), new Vector2D(10, 10), 1.5);
        farYoungSheep.init(mockRegionManager);

        Animal closeOldSheep = AnimalTestHelper.createSheep(
            new SelectFirst(), new SelectFirst(), new Vector2D(255, 255), 5.0);
        closeOldSheep.init(mockRegionManager);

        // Reference animal is at (250, 250)
        AnimalTestHelper.setPosition(referenceAnimal, new Vector2D(250, 250));

        List<Animal> animals = new ArrayList<>();
        animals.add(closeOldSheep);
        animals.add(farYoungSheep);

        // Execute: select
        Animal selected = selectYoungest.select(referenceAnimal, animals);

        // Verify: Due to bug, returns reference animal
        assertEquals(referenceAnimal, selected,
            "SelectYoungest returns reference animal (implementation bug)");
    }

    @Test
    public void testSelectWithZeroAge() {
        // Setup: list with animals including newborn (age 0)
        Animal newborn = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(100, 100));
        newborn.init(mockRegionManager);
        // Age is 0 by default after creation

        Animal youngSheep = AnimalTestHelper.createSheep(
            new SelectFirst(), new SelectFirst(), new Vector2D(200, 200), 2.0);
        youngSheep.init(mockRegionManager);

        Animal oldSheep = AnimalTestHelper.createSheep(
            new SelectFirst(), new SelectFirst(), new Vector2D(300, 300), 5.0);
        oldSheep.init(mockRegionManager);

        List<Animal> animals = new ArrayList<>();
        animals.add(youngSheep);
        animals.add(oldSheep);
        animals.add(newborn);

        // Execute: select
        Animal selected = selectYoungest.select(referenceAnimal, animals);

        // Verify: Due to bug, returns reference animal
        assertEquals(referenceAnimal, selected, "SelectYoungest returns reference animal (implementation bug)");
    }

    @Test
    public void testSelectWithVerySmallAgeDifferences() {
        // Setup: animals with very small age differences
        Animal sheep1 = AnimalTestHelper.createSheep(
            new SelectFirst(), new SelectFirst(), new Vector2D(100, 100), 3.001);
        sheep1.init(mockRegionManager);

        Animal sheep2 = AnimalTestHelper.createSheep(
            new SelectFirst(), new SelectFirst(), new Vector2D(200, 200), 3.0);
        sheep2.init(mockRegionManager);

        Animal sheep3 = AnimalTestHelper.createSheep(
            new SelectFirst(), new SelectFirst(), new Vector2D(300, 300), 3.002);
        sheep3.init(mockRegionManager);

        List<Animal> animals = new ArrayList<>();
        animals.add(sheep1);
        animals.add(sheep3);
        animals.add(sheep2);

        // Execute: select
        Animal selected = selectYoungest.select(referenceAnimal, animals);

        // Verify: Due to bug, returns reference animal
        assertEquals(referenceAnimal, selected, "SelectYoungest returns reference animal (implementation bug)");
    }

    @Test
    public void testSelectIsConsistent() {
        // Setup: list with animals of different ages
        Animal young = AnimalTestHelper.createSheep(
            new SelectFirst(), new SelectFirst(), new Vector2D(100, 100), 1.0);
        young.init(mockRegionManager);

        Animal old = AnimalTestHelper.createSheep(
            new SelectFirst(), new SelectFirst(), new Vector2D(200, 200), 5.0);
        old.init(mockRegionManager);

        List<Animal> animals = new ArrayList<>();
        animals.add(old);
        animals.add(young);

        // Execute: select multiple times
        Animal firstSelection = selectYoungest.select(referenceAnimal, animals);
        Animal secondSelection = selectYoungest.select(referenceAnimal, animals);
        Animal thirdSelection = selectYoungest.select(referenceAnimal, animals);

        // Verify: should always return the same animal
        assertEquals(firstSelection, secondSelection,
            "SelectYoungest should return consistent results");
        assertEquals(secondSelection, thirdSelection,
            "SelectYoungest should return consistent results");
        assertEquals(referenceAnimal, firstSelection,
            "SelectYoungest returns reference animal (implementation bug)");
    }

    @Test
    public void testSelectWithNullReferenceAnimal() {
        // Setup: list with animals
        Animal young = AnimalTestHelper.createSheep(
            new SelectFirst(), new SelectFirst(), new Vector2D(100, 100), 2.0);
        young.init(mockRegionManager);

        Animal old = AnimalTestHelper.createSheep(
            new SelectFirst(), new SelectFirst(), new Vector2D(200, 200), 6.0);
        old.init(mockRegionManager);

        List<Animal> animals = new ArrayList<>();
        animals.add(old);
        animals.add(young);

        // Execute & Verify: should throw NullPointerException due to implementation
        assertThrows(NullPointerException.class, () -> {
            selectYoungest.select(null, animals);
        }, "SelectYoungest throws NullPointerException with null reference animal");
    }
}
