import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * A collection of items with assigned weights that can easily calculate weighted
 * probabilities.
 * - Based on https://stackoverflow.com/a/6409791
 *
 * @param <E> The type of object being collected.
 */
public class RandomCollection<E> {
    private NavigableMap<Float, E> map;
    private float total;

    /**
     * Default constructor that creates an empty RandomCollection.
     */
    public RandomCollection() {
        this.map = new TreeMap<>();
        this.total = 0.0f;
    }

    /**
     * Copy constructor that creates a deep copy of another RandomCollection.
     *
     * @param other The RandomCollection to copy.
     */
    public RandomCollection(RandomCollection<E> other) {
        this.map = new TreeMap<>(other.map);
        this.total = other.total;

    }

    /**
     * Adds an item to the RandomCollection with the given weight.
     *
     * @param weight The weight of the item.
     * @param result The item to add.
     * @return Itself for method chaining.
     */
    public RandomCollection<E> add(float weight, E result) {
        if(weight <= 0) {
            throw new IllegalArgumentException("Cannot set negative weight");
        }
        addItem(weight, result);
        return this;
    }

    /**
     * Adds multiple items to the RandomCollection, all with the same weight.
     *
     * @param weight The weight of the items.
     * @param results The items to add.
     * @return Itself for method chaining.
     */
    public RandomCollection<E> addAll(float weight, List<E> results) {
        if(weight <= 0) {
            throw new IllegalArgumentException("Cannot set negative weight");
        }
        for(E result : results) {
            addItem(weight, result);
        }
        return this;
    }

    /**
     * Generates a random item from this RandomCollection.
     *
     * @return A random item from this RandomCollection, or null if the collection is empty.
     */
    public E next() {
        if(map.size() <= 0) {
            return null;
        }
        float value = (float) (Math.random() * total);
        return map.higherEntry(value).getValue();
    }

    /**
     * Returns whether this RandomCollection contains a given item.
     *
     * @param item The item to check.
     * @return True if the RandomCollection contains the item, otherwise false.
     */
    public boolean hasItem(E item) {
        return map.containsValue(item);
    }

    /**
     * Helper method for adding an item to the RandomCollection.
     *
     * @param weight The weight of the item.
     * @param result The item to add.
     */
    private void addItem(float weight, E result) {
        total += weight;
        map.put(total, result);
    }

    /**
     * Returns the size of this RandomCollection.
     *
     * @return The size of this RandomCollection.
     */
    public int size() {
        return map.size();
    }

    /**
     * Returns whether this random collection is empty or not.
     *
     * @return true if empty, otherwise false
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public String toString() {
        return map.toString();
    }
}
