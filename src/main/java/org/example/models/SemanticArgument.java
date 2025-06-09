package org.example.models;

import java.util.Arrays;
import java.util.Objects;

public class SemanticArgument {
    public String predicate;
    public Argument[] arguments;

    public static class Argument {
        public String type, value;

        @Override
        public String toString() {
            return "Type: " + type + ", Value: " + value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, value);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Predicate: ").append(predicate).append("\n");

        if (arguments != null && arguments.length > 0) {
            sb.append("Arguments:\n");
            for (Argument arg : arguments) {
                sb.append("  - Type: ").append(arg.type)
                        .append(", Value: ").append(arg.value).append("\n");
            }
        }

        return sb.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(predicate, Arrays.hashCode(arguments));
    }
}
