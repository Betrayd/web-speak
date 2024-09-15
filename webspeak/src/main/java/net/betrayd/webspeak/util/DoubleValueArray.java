package net.betrayd.webspeak.util;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

public class DoubleValueArray<T> {
    private final List<T[]> values = new ArrayList<>();

    //TODO: learn how to supress warning here too
    public boolean add(@NotNull T value1,@NotNull T value2)
    {
        if(!hasValueSet(value1, value2))
        {
            List<T> section = new ArrayList<>();
                section.add(value1);
                section.add(value2);
            values.add((T[])section.toArray());
            return true;
        }
        return false;
    }

    public boolean remove(@NotNull T value1, @NotNull T value2)
    {
        List<Integer> indexes = assosiatedValueIndexes(value1);
        for(Integer i : indexes)
        {
            T[] curValue = values.get(i);

            if(curValue[0] == value2 || curValue[1] == value2)
            {
                values.remove(i.intValue());
                return true;
            }
        }
        return false;
    }

    public boolean hasValueSet(@NotNull T value,@NotNull T otherValue)
    {
        if(assosiatedValues(value).contains(otherValue))
        {
            return true;
        }
        return false;
    }

    public List<T> assosiatedValues(@NotNull T inputValue)
    {
        List<T> returnValues = new ArrayList<>();

        List<Integer> indexes = assosiatedValueIndexes(inputValue);
        for(Integer i : indexes)
        {
            T[] curValue = values.get(i);

            if(curValue[0] == inputValue)
            {
                returnValues.add(curValue[1]);
            }
            else if(curValue[1] == inputValue)
            {
                returnValues.add(curValue[0]);
            }
        }

        return returnValues;
    }

    public List<Integer> assosiatedValueIndexes(@NotNull T inputValue)
    {
        List<Integer> returnIndexes = new ArrayList<>();

        for(int i = 0; i < values.size(); i++)
        {
            T[] set = values.get(i);

            if(set[0] == inputValue || set[1] == inputValue)
            {
                returnIndexes.add(i);
            }
        }

        return returnIndexes;
    }
}
