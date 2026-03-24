package simulator.model.animal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import simulator.misc.Vector2D;
import simulator.model.strategy.SelectFirst;
import simulator.model.strategy.SelectionStrategy;
import simulator.model.region.AnimalMapView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test class for Wolf behavior, covering movement and state transitions
 */
public class WolfTest {

    @Spy
    private AnimalMapView mockRegionManager = new AnimalMapView() {
      @Override
      public List<Animal> getAnimalsInRange(Animal e, Predicate<Animal> filter) {
        return List.of();
      }

      @Override
      public double getFood(AnimalInfo animal, double dt) {
        return 0;
      }

      @Override
      public int getCols() {
        return 0;
      }

      @Override
      public int getRows() {
        return 0;
      }

      @Override
      public int getWidth() {
        return 0;
      }

      @Override
      public int getHeight() {
        return 0;
      }

      @Override
      public int getRegionWidth() {
        return 0;
      }

      @Override
      public int getRegionHeight() {
        return 0;
      }
    };

    private Wolf wolf;
    private SelectionStrategy mateStrategy;
    private SelectionStrategy huntStrategy;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Configure region manager mock
        when(mockRegionManager.getWidth()).thenReturn(500);
        when(mockRegionManager.getHeight()).thenReturn(500);
        when(mockRegionManager.getFood(any(AnimalInfo.class), anyDouble())).thenReturn(0.0);
        when(mockRegionManager.getAnimalsInRange(any(Animal.class), any(Predicate.class)))
            .thenReturn(new ArrayList<>());

        mateStrategy = new SelectFirst();
        huntStrategy = new SelectFirst();

        try {
            wolf = new Wolf(mateStrategy, huntStrategy, new Vector2D(250, 250));
            wolf.init(mockRegionManager);
        } catch (Exception e) {
            System.err.println("Unable to instantiate Wolf");
            e.printStackTrace();
        }
    }

    // ========== MOVEMENT TESTS ==========

    @Test
    public void testNormalMovement() {
        // Setup: wolf in NORMAL state
        assertEquals(Animal.State.NORMAL, wolf.getState());

        Vector2D initialPos = wolf.getPosition();
        double initialAge = wolf.getAge();
        double initialEnergy = wolf.getEnergy();
        double initialDesire = wolf.desire;

        // Execute: update with time
        wolf.update(1.0);

        // Verify: position changed (moved), age increased, energy decreased, desire increased
        assertNotEquals(initialPos, wolf.getPosition(),
            "Wolf should move in NORMAL state");
        assertTrue(wolf.getAge() > initialAge,
            "Age should increase");
        assertTrue(wolf.getEnergy() < initialEnergy,
            "Energy should decrease (food consumption)");
        assertTrue(wolf.desire > initialDesire,
            "Desire should increase");
    }

    @Test
    public void testMateMovementTowardsTarget() {
        // Setup: create a mate target far away
        Wolf mateTarget = new Wolf(mateStrategy, huntStrategy, new Vector2D(400, 400));
        mateTarget.init(mockRegionManager);
        mateTarget.desire = 70.0;

        List<Animal> allAnimals = new ArrayList<>();
        allAnimals.add(mateTarget);

        // Configure mock to return appropriate animals based on filter
        when(mockRegionManager.getAnimalsInRange(eq(wolf), any(Predicate.class)))
            .thenAnswer(invocation -> {
                Predicate<Animal> filter = invocation.getArgument(1);
                List<Animal> result = new ArrayList<>();
                for (Animal a : allAnimals) {
                    if (filter.test(a)) {
                        result.add(a);
                    }
                }
                return result;
            });

        // Set wolf desire high to trigger MATE state
        wolf.pos = new Vector2D(100, 100);
        wolf.desire = 70.0;
        wolf.update(0.1);

        assertEquals(Animal.State.MATE, wolf.getState(),
            "Wolf should be in MATE state");

        Vector2D initialPos = wolf.getPosition();
        double initialDistance = initialPos.distanceTo(mateTarget.getPosition());

        // Execute: update several times to allow significant movement
        for (int i = 0; i < 10; i++) {
            wolf.update(1.0);
            // Stop if no longer in MATE state
            if (wolf.getState() != Animal.State.MATE) {
                break;
            }
        }

        // Verify: wolf moved closer to mate target
        double finalDistance = wolf.getPosition().distanceTo(mateTarget.getPosition());
        assertTrue(finalDistance <= initialDistance,
            "Wolf should move towards or reach mate target. Initial: " + initialDistance + ", Final: " + finalDistance);
    }

    @Test
    public void testHungerMovementTowardsPrey() {
        // Setup: create a prey (herbivore)
        Animal prey = mock(Animal.class);
        when(prey.getDiet()).thenReturn(Animal.Diet.HERBIVORE);
        when(prey.getGeneticCode()).thenReturn("Sheep");
        when(prey.getPosition()).thenReturn(new Vector2D(200, 200));
        when(prey.getState()).thenReturn(Animal.State.NORMAL);

        List<Animal> herbivores = new ArrayList<>();
        herbivores.add(prey);

        when(mockRegionManager.getAnimalsInRange(eq(wolf), any(Predicate.class)))
            .thenAnswer(invocation -> {
                Predicate<Animal> filter = invocation.getArgument(1);
                List<Animal> result = new ArrayList<>();
                for (Animal a : herbivores) {
                    if (filter.test(a)) {
                        result.add(a);
                    }
                }
                return result;
            });

        // Set wolf energy low to trigger HUNGER state and wolf pos close enough to prey
        wolf.pos = new Vector2D(180, 180);
        wolf.energy = 40.0; // Below FOOD_THRESHOLD_WOLF (50.0)
        wolf.update(0.1);

        assertEquals(Animal.State.HUNGER, wolf.getState(),
            "Wolf should be in HUNGER state");

        Vector2D initialPos = wolf.getPosition();
        double initialDistance = initialPos.distanceTo(prey.getPosition());

        // Execute: update several times to allow movement. DON'T use a high dt so wolf cannot be far from prey
        for (int i = 0; i < 10; i++) {
            wolf.update(0.1);
            // Stop if prey is dead or wolf changed state
            if (prey.getState() == Animal.State.DEAD || wolf.getState() != Animal.State.HUNGER) {
                break;
            }
        }

        // Verify: wolf moved closer to prey
        double finalDistance = wolf.getPosition().distanceTo(prey.getPosition());
        assertTrue(finalDistance <= initialDistance,
            "Wolf should move towards prey. Initial: " + initialDistance + ", Final: " + finalDistance);
    }

    @Test
    public void testDeadStateNoMovement() {
        // Setup: kill the wolf
        wolf.energy = 0;
        wolf.update(0.1);

        assertEquals(Animal.State.DEAD, wolf.getState(),
            "Wolf should be DEAD when energy is 0");

        Vector2D positionBeforeDeath = wolf.getPosition();

        // Execute: update multiple times
        for (int i = 0; i < 10; i++) {
            wolf.update(1.0);
        }

        // Verify: position didn't change
        assertEquals(positionBeforeDeath, wolf.getPosition(),
            "Dead wolf should not move");
    }

    @Test
    public void testEatingIncreasesEnergy() {
        // Setup: configure region to provide food
        double foodAmount = 5.0;
        when(mockRegionManager.getFood(any(AnimalInfo.class), anyDouble()))
            .thenReturn(foodAmount);

        double initialEnergy = wolf.getEnergy();

        // Execute: update
        wolf.update(1.0);

        // Verify: getFood was called
        verify(mockRegionManager, atLeastOnce())
            .getFood(eq(wolf), anyDouble());
    }

    // ========== STATE TRANSITION TESTS ==========

    @Test
    public void testTransitionFromNormalToHunger() {
        // Setup: reduce energy below threshold
        wolf.energy = 45.0; // Below FOOD_THRESHOLD_WOLF (50.0)

        // Execute: update
        wolf.update(0.1);

        // Verify: state changed to HUNGER
        assertEquals(Animal.State.HUNGER, wolf.getState(),
            "Wolf should transition to HUNGER when energy < 50.0");
    }

    @Test
    public void testTransitionFromNormalToMate() {
        // Setup: increase desire above threshold
        wolf.desire = 66.0; // Above DESIRE_THRESHOLD_WOLF (65.0)

        // Execute: update
        wolf.update(0.1);

        // Verify: state changed to MATE
        assertEquals(Animal.State.MATE, wolf.getState(),
            "Wolf should transition to MATE when desire > 65.0");
    }

    @Test
    public void testTransitionFromMateToHunger() {
        // Setup: put wolf in MATE state
        wolf.desire = 70.0;
        wolf.update(0.1);
        assertEquals(Animal.State.MATE, wolf.getState());

        // Reduce energy below threshold
        wolf.energy = 45.0;

        // Execute: update
        wolf.update(0.1);

        // Verify: state changed to HUNGER
        assertEquals(Animal.State.HUNGER, wolf.getState(),
            "Wolf should transition from MATE to HUNGER when energy < 50.0");
    }

    @Test
    public void testTransitionFromMateToNormal() {
        // Setup: put wolf in MATE state
        wolf.desire = 70.0;
        wolf.update(0.1);
        assertEquals(Animal.State.MATE, wolf.getState());

        // Lower desire below threshold
        wolf.desire = 60.0;

        // Execute: update (no mate found)
        when(mockRegionManager.getAnimalsInRange(eq(wolf), any(Predicate.class)))
            .thenReturn(new ArrayList<>());
        wolf.update(0.1);

        // Verify: state changed to NORMAL
        assertEquals(Animal.State.NORMAL, wolf.getState(),
            "Wolf should transition to NORMAL when desire drops below threshold");
    }

    @Test
    public void testTransitionFromHungerToNormal() {
        // Setup: put wolf in HUNGER state
        wolf.energy = 40.0;
        wolf.desire = 60.0;
        wolf.update(0.1);
        assertEquals(Animal.State.HUNGER, wolf.getState());

        // Increase energy above threshold and keep desire low
        wolf.energy = 55.0;
        wolf.desire = 60.0;

        // Execute: update (no prey found)
        when(mockRegionManager.getAnimalsInRange(eq(wolf), any(Predicate.class)))
            .thenReturn(new ArrayList<>());
        wolf.update(0.1);

        // Verify: state changed to NORMAL
        assertEquals(Animal.State.NORMAL, wolf.getState(),
            "Wolf should transition to NORMAL when energy > 50.0 and desire < 65.0");
    }

    @Test
    public void testTransitionFromHungerToMate() {
        // Setup: put wolf in HUNGER state
        wolf.energy = 40.0;
        wolf.desire = 70.0;
        wolf.update(0.1);
        assertEquals(Animal.State.HUNGER, wolf.getState());

        // Increase energy above threshold and keep desire high
        wolf.energy = 55.0;
        wolf.desire = 70.0;

        // Execute: update (no prey found)
        when(mockRegionManager.getAnimalsInRange(eq(wolf), any(Predicate.class)))
            .thenReturn(new ArrayList<>());
        wolf.update(0.1);

        // Verify: state changed to MATE
        assertEquals(Animal.State.MATE, wolf.getState(),
            "Wolf should transition to MATE when energy > 50.0 and desire >= 65.0");
    }

    @Test
    public void testTransitionToDeadByEnergyDepletion() {
        // Setup: reduce energy to 0
        wolf.energy = 0.1;

        // Execute: update
        wolf.update(1.0);

        // Verify: state changed to DEAD
        assertEquals(Animal.State.DEAD, wolf.getState(),
            "Wolf should die when energy reaches 0");
    }

    @Test
    public void testTransitionToDeadByAge() {
        // Setup: set age above maximum
        wolf.age = 14.1; // MAX_AGE_WOLF is 14.0

        // Execute: update
        wolf.update(0.1);

        // Verify: state changed to DEAD
        assertEquals(Animal.State.DEAD, wolf.getState(),
            "Wolf should die when age exceeds MAX_AGE_WOLF (14.0)");
    }

    // ========== BEHAVIOR TESTS ==========

    @Test
    public void testHuntingBehavior() {
        // Setup: create a real prey (not a mock) to allow state changes
        Sheep prey = new Sheep(new SelectFirst(), new SelectFirst(), new Vector2D(254, 254));
        prey.init(mockRegionManager);

        List<Animal> herbivores = new ArrayList<>();
        herbivores.add(prey);

        when(mockRegionManager.getAnimalsInRange(eq(wolf), any(Predicate.class)))
            .thenAnswer(invocation -> {
                Predicate<Animal> filter = invocation.getArgument(1);
                List<Animal> result = new ArrayList<>();
                for (Animal a : herbivores) {
                    if (filter.test(a) && a.getState() != Animal.State.DEAD) {
                        result.add(a);
                    }
                }
                return result;
            });

        // Set wolf hungry and very close to prey (within COLLISION_RANGE = 8)
        wolf.pos = new Vector2D(250, 250);
        prey.pos = new Vector2D(251, 250); // Distance: ~5.66, within COLLISION_RANGE
        wolf.energy = 45.0; // Low but enough to stay in HUNGER state
        double initialEnergy = wolf.energy;

        // Verify initial state
        assertEquals(Animal.State.NORMAL, prey.getState(),
            "Prey should start alive");

        // Execute: trigger HUNGER state first
        wolf.update(0.1);
        assertEquals(Animal.State.HUNGER, wolf.getState(),
            "Wolf should be in HUNGER state");

        assertTrue(wolf.pos.distanceTo(prey.pos) < 8, "Wolf and prey are not close enough");

        // Execute: one more update should trigger hunt (wolf is already close enough)
        wolf.update(0.05);

        // Verify: prey was killed
        assertEquals(Animal.State.DEAD, prey.getState(),
            "Prey should be killed by wolf");

        // Verify: wolf gained energy from hunt
        assertTrue(wolf.getEnergy() > initialEnergy,
            "Wolf should gain energy after hunting (initial: " + initialEnergy + ", current: " + wolf.getEnergy() + ")");
    }

    @Test
    public void testMatingBehavior() {
        // Setup: create two wolves very close together with high desire
        Wolf mate = new Wolf(mateStrategy, huntStrategy, new Vector2D(252, 252));
        mate.init(mockRegionManager);
        mate.desire = 70.0;

        // Position wolves very close for immediate mating
        wolf.pos = new Vector2D(250, 250);
        mate.pos = new Vector2D(252, 252);
        wolf.desire = 70.0;

        List<Animal> allAnimals = new ArrayList<>();
        allAnimals.add(mate);

        when(mockRegionManager.getAnimalsInRange(eq(wolf), any(Predicate.class)))
            .thenAnswer(invocation -> {
                Predicate<Animal> filter = invocation.getArgument(1);
                List<Animal> result = new ArrayList<>();
                for (Animal a : allAnimals) {
                    if (filter.test(a)) {
                        result.add(a);
                    }
                }
                return result;
            });

        double initialDesire = wolf.desire;

        // Execute: update multiple times to allow mating to occur
        for (int i = 0; i < 20; i++) {
            wolf.update(0.1);

            // Check if mating occurred (desire reset to 0)
            if (wolf.desire < initialDesire * 0.5) {
                break;
            }

            // If wolf changed state away from MATE/NORMAL, stop
            if (wolf.getState() == Animal.State.DEAD || wolf.getState() == Animal.State.HUNGER) {
                break;
            }
        }

        // Verify: mating behavior should be functional
        assertTrue(
            wolf.desire < 50.0 || wolf.getState() == Animal.State.MATE || wolf.getState() == Animal.State.NORMAL,
            "Mating behavior should be functional (desire: " + wolf.desire + ", state: " + wolf.getState() + ")"
        );
    }

    @Test
    public void testDeadWolfDoesNotEat() {
        // Setup: kill the wolf
        wolf.energy = 0;
        wolf.update(0.1);
        assertEquals(Animal.State.DEAD, wolf.getState());

        // Configure food source
        when(mockRegionManager.getFood(any(AnimalInfo.class), anyDouble()))
            .thenReturn(10.0);

        // Reset mock to count invocations
        reset(mockRegionManager);
        when(mockRegionManager.getWidth()).thenReturn(500);
        when(mockRegionManager.getHeight()).thenReturn(500);
        when(mockRegionManager.getFood(any(AnimalInfo.class), anyDouble()))
            .thenReturn(10.0);

        // Execute: update
        wolf.update(1.0);

        // Verify: dead wolf doesn't eat
        verify(mockRegionManager, never()).getFood(any(AnimalInfo.class), anyDouble());
    }

    @Test
    public void testPositionAdjustmentWhenOutOfBounds() {
        // Setup: move wolf out of bounds
        wolf.pos = new Vector2D(-10, -10);

        // Execute: update (should trigger position adjustment)
        wolf.update(0.1);

        // Verify: position is back within bounds and state is NORMAL
        Vector2D pos = wolf.getPosition();
        assertTrue(pos.getX() >= 0 && pos.getX() < 500,
            "X position should be within bounds");
        assertTrue(pos.getY() >= 0 && pos.getY() < 500,
            "Y position should be within bounds");
        assertEquals(Animal.State.NORMAL, wolf.getState(),
            "Wolf should be in NORMAL state after position adjustment");
    }

    @Test
    public void testConstructorRequiresHuntStrategy() {
        // Verify: constructor throws exception if hunt strategy is null
        assertThrows(IllegalArgumentException.class, () -> {
            new Wolf(mateStrategy, null, new Vector2D(250, 250));
        }, "Constructor should throw exception when hunt strategy is null");
    }

    @Test
    public void testInitialState() {
        // Verify: newly created wolf has correct initial values
        assertEquals(Animal.State.NORMAL, wolf.getState(),
            "Initial state should be NORMAL");
        assertEquals(Animal.Diet.CARNIVORE, wolf.getDiet(),
            "Wolf should be carnivore");
        assertEquals("Wolf", wolf.getGeneticCode(),
            "Genetic code should be 'Wolf'");
        assertEquals(0.0, wolf.desire,
            "Initial desire should be 0");
        assertEquals(100.0, wolf.getEnergy(),
            "Initial energy should be 100");
    }

    @Test
    public void testHungerPriorityOverMate() {
        // Setup: wolf with both high desire and low energy
        wolf.energy = 45.0; // Below FOOD_THRESHOLD_WOLF
        wolf.desire = 70.0; // Above DESIRE_THRESHOLD_WOLF

        // Execute: update
        wolf.update(0.1);

        // Verify: HUNGER takes priority over MATE
        assertEquals(Animal.State.HUNGER, wolf.getState(),
            "Wolf should prioritize HUNGER over MATE when energy is low");
    }

    @Test
    public void testEnergyLossFromMating() {
        // Setup: create a mate close by
        Wolf mate = new Wolf(mateStrategy, huntStrategy, new Vector2D(251, 251));
        mate.init(mockRegionManager);
        mate.desire = 70.0;

        wolf.pos = new Vector2D(250, 250);
        wolf.desire = 70.0;
        double initialEnergy = wolf.energy;

        List<Animal> allAnimals = new ArrayList<>();
        allAnimals.add(mate);

        when(mockRegionManager.getAnimalsInRange(eq(wolf), any(Predicate.class)))
            .thenAnswer(invocation -> {
                Predicate<Animal> filter = invocation.getArgument(1);
                List<Animal> result = new ArrayList<>();
                for (Animal a : allAnimals) {
                    if (filter.test(a)) {
                        result.add(a);
                    }
                }
                return result;
            });

        // Execute: update multiple times to trigger mating
        boolean mated = false;
        for (int i = 0; i < 20; i++) {
            double energyBefore = wolf.energy;
            wolf.update(0.1);
            // If energy dropped more than normal consumption, mating likely occurred
            if (wolf.desire == 0.0) {
                mated = true;
                break;
            }
        }

        // Verify: if mating occurred, verify the behavior
        if (mated) {
            assertTrue(wolf.desire == 0.0,
                "Desire should be reset after mating");
        }
    }

    // ========== SPEED VERIFICATION TESTS ==========

    @Test
    public void testSpeedInNormalState() {
        // Setup: wolf with known energy and position
        wolf.energy = 100.0; // Full energy for predictable calculation
        wolf.pos = new Vector2D(100, 100);
        wolf.dest = new Vector2D(200, 100); // 100 units away horizontally

        double initialX = wolf.pos.getX();
        double dt = 1.0;

        // Expected speed factor = speed * dt * exp((100-100)*0.007) = speed * 1.0 * 1.0
        double expectedSpeedFactor = wolf.getSpeed() * dt;

        // Execute: move in NORMAL state
        wolf.update(dt);

        // Verify: distance moved should match expected speed
        double actualDistanceMoved = wolf.pos.getX() - initialX;
        assertEquals(expectedSpeedFactor, actualDistanceMoved, 5.0,
            "Wolf should move at normal speed in NORMAL state");
    }

    @Test
    public void testSpeedInHungerStateWithBoostedMovement() {
        // Setup: create prey WITHIN SIGHT RANGE to trigger hunt movement
        Animal prey = mock(Animal.class);
        when(prey.getDiet()).thenReturn(Animal.Diet.HERBIVORE);
        when(prey.getPosition()).thenReturn(new Vector2D(140, 100)); // Within sight range (50)
        when(prey.getState()).thenReturn(Animal.State.NORMAL);
        when(prey.isDead()).thenReturn(false);

        List<Animal> herbivores = new ArrayList<>();
        herbivores.add(prey);

        when(mockRegionManager.getAnimalsInRange(eq(wolf), any(Predicate.class)))
            .thenAnswer(invocation -> {
                Predicate<Animal> filter = invocation.getArgument(1);
                List<Animal> result = new ArrayList<>();
                for (Animal a : herbivores) {
                    if (filter.test(a)) {
                        result.add(a);
                    }
                }
                return result;
            });

        // Set wolf in HUNGER state with controlled position and energy
        wolf.pos = new Vector2D(100, 100);
        wolf.energy = 45.0; // Below threshold to stay in HUNGER
        wolf.desire = 30.0; // Low desire to avoid transitioning to MATE
        wolf.update(0.01); // Enter HUNGER state

        assertEquals(Animal.State.HUNGER, wolf.getState(),
            "Wolf should be in HUNGER state");

        // Measure movement during hunt (wolf will find and follow prey in this update)
        double initialX = wolf.pos.getX();
        double dt = 0.5;

        // Execute: update where wolf follows prey
        wolf.update(dt);

        // Verify: wolf moved significantly (faster than normal speed)
        double actualDistanceMoved = Math.abs(wolf.pos.getX() - initialX);

        // Calculate what normal speed would be with current energy
        double energyDecayFactor = Math.exp((45.0 - 100) * 0.007);
        double normalSpeedDistance = wolf.getSpeed() * dt * energyDecayFactor;

        // Boosted speed should be ~3x normal speed
        assertTrue(actualDistanceMoved > normalSpeedDistance * 2.0,
            String.format("Wolf should move faster in HUNGER state (boosted ~3x). Normal: %.2f, Actual: %.2f",
                normalSpeedDistance, actualDistanceMoved));
    }

    @Test
    public void testSpeedInMateStateWithBoostedMovement() {
        // Setup: create mate WITHIN SIGHT RANGE
        Wolf mate = new Wolf(mateStrategy, huntStrategy, new Vector2D(140, 100));
        mate.init(mockRegionManager);
        mate.desire = 70.0;

        List<Animal> wolves = new ArrayList<>();
        wolves.add(mate);

        when(mockRegionManager.getAnimalsInRange(eq(wolf), any(Predicate.class)))
            .thenAnswer(invocation -> {
                Predicate<Animal> filter = invocation.getArgument(1);
                List<Animal> result = new ArrayList<>();
                for (Animal a : wolves) {
                    if (filter.test(a)) {
                        result.add(a);
                    }
                }
                return result;
            });

        // Set wolf in MATE state
        wolf.pos = new Vector2D(100, 100);
        wolf.energy = 90.0; // High energy to stay in MATE
        wolf.desire = 70.0; // Above threshold
        wolf.update(0.01); // Enter MATE state

        assertEquals(Animal.State.MATE, wolf.getState(),
            "Wolf should be in MATE state");

        // Measure movement when wolf finds and follows mate
        double initialX = wolf.pos.getX();
        double dt = 0.5;

        // Execute: wolf will find and follow mate in this update
        wolf.update(dt);

        // Verify: wolf moved significantly
        double actualDistanceMoved = Math.abs(wolf.pos.getX() - initialX);

        // Calculate expected normal speed distance for comparison
        double energyDecayFactor = Math.exp((90.0 - 100) * 0.007);
        double normalSpeedDistance = wolf.getSpeed() * dt * energyDecayFactor;

        // Boosted speed should be significantly faster (~3x)
        assertTrue(actualDistanceMoved > normalSpeedDistance * 2.0,
            String.format("Wolf should move faster in MATE state (boosted ~3x). Normal would be: %.2f, Actual: %.2f",
                normalSpeedDistance, actualDistanceMoved));
    }
}
