package org.example.models;

import java.util.Arrays;
import java.util.Objects;

public class VerbDetails {
    public String verbNetId;
    public ThematicRole[] thematicRoles;
    public SemanticArgument[] semanticArguments;

    public static class ThematicRole {
        public String type;
        public Integer wordIndex;

        @Override
        public String toString() {
            return "Type: " + type + ", Word Index: " + wordIndex;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, wordIndex);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("VerbNet ID: ").append(verbNetId).append("\n");

        if (thematicRoles != null && thematicRoles.length > 0) {
            sb.append("Thematic Roles:\n");
            for (ThematicRole role : thematicRoles) {
                sb.append("  - Type: ").append(role.type)
                        .append(", Word Index: ").append(role.wordIndex).append("\n");
            }
        }

        if (semanticArguments != null && semanticArguments.length > 0) {
            sb.append("Semantic Arguments:\n");
            for (SemanticArgument arg : semanticArguments) {
                sb.append(arg.toString().indent(2));
            }
        }

        return sb.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                Arrays.hashCode(thematicRoles),
                Arrays.hashCode(semanticArguments));
    }
}
