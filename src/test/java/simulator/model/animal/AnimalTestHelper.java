package simulator.model.animal;

import simulator.misc.Vector2D;
import simulator.model.strategy.SelectionStrategy;

/**
 * Helper class for testing that provides access to protected animal fields
 */
public class AnimalTestHelper {

    public static void setAge(Animal animal, double age) {
        animal.age = age;
    }

    public static void setDesire(Animal animal, double desire) {
        animal.desire = desire;
    }

    public static void setEnergy(Animal animal, double energy) {
        animal.energy = energy;
    }

    public static void setPosition(Animal animal, Vector2D position) {
        animal.pos = position;
    }

    public static double getDesire(Animal animal) {
        return animal.desire;
    }

    public static Sheep createSheep(SelectionStrategy mateStrategy, SelectionStrategy dangerStrategy,
                                     Vector2D position, double age) {
        Sheep sheep = new Sheep(mateStrategy, dangerStrategy, position);
        sheep.age = age;
        return sheep;
    }

    public static Wolf createWolf(SelectionStrategy huntStrategy, SelectionStrategy mateStrategy,
                                   Vector2D position, double age) {
        Wolf wolf = new Wolf(huntStrategy, mateStrategy, position);
        wolf.age = age;
        return wolf;
    }
}
