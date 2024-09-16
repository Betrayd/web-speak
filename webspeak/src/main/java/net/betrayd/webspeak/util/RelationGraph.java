package net.betrayd.webspeak.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RelationGraph<T> {
    private Map<T, Set<T>> relationMap = new HashMap<>();
    
    /**
     * Add a relation
     * @param a Value A
     * @param b Value B
     * @return If the relation did not already exist
     */
    public boolean add(T a, T b) {
        if (containsRelation(a, b))
            return false;

        relationMap.computeIfAbsent(a, x -> new HashSet<>()).add(b);
        relationMap.computeIfAbsent(b, x -> new HashSet<>()).add(a);
        return true;
    }

    /**
     * Remove a relation between two values.
     * @param a Value A
     * @param b Value B
     * @return If the relation was there and could be removed.
     */
    public boolean remove(Object a, Object b) {
        boolean success = false;
        Set<T> set = relationMap.get(a);
        if (set != null && set.remove(b)) {

            success = true;
            if (set.isEmpty())
                relationMap.remove(a);

        }

        Set<T> set2 = relationMap.get(b);
        if (set2 != null && set2.remove(a)) {
            success = true;
            if (set2.isEmpty())
                relationMap.remove(b);
        }

        return success;
    }

    /**
     * Remove all relations of a given value.
     * @param value Value to remove
     * @return If any relation was found.
     */
    public boolean remove(Object value) {
        Set<T> set = relationMap.remove(value);
        if (set == null)
            return false;

        for (T val : set) {
            Set<T> otherSet = relationMap.get(val);
            if (otherSet != null)
                otherSet.remove(value);
        }
        return true;
    }

    /**
     * Check if a relation exists
     * @param a Value A
     * @param b Value B
     * @return If values A and B have a relation
     */
    public boolean containsRelation(T a, T b) {
        Set<T> aConnections = relationMap.get(a);
        return aConnections != null && aConnections.contains(b);
    }

    /**
     * Get a set of all values that a given value has a relation to.
     * @param value Value to check.
     * @return Unmodifiable set of all relations.
     */
    public Set<T> getRelations(Object value) {
        Set<T> relations = relationMap.get(value);
        if (relations != null) {
            return Collections.unmodifiableSet(relations);
        } else {
            return Collections.emptySet();
        }
    }
}
