package patchlib.agent.spec;

public record AdviceSpec(AdviceType adviceType) implements PatchSpec {

    public enum AdviceType {
        BEFORE, AFTER, EXCEPT
    }

}
