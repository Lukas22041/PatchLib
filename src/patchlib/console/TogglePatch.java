package patchlib.console;

import org.lazywizard.console.BaseCommandWithSuggestion;
import org.lazywizard.console.Console;
import patchlib.agent.patch.PatchStateRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Console command to toggle the state of one or more patches */
public class TogglePatch implements BaseCommandWithSuggestion {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (args.trim().isEmpty()) return CommandResult.BAD_SYNTAX;

        Set<String> patchClassNames = new LinkedHashSet<>(Arrays.asList(args.trim().split("\\s+")));
        List<String> knownPatchClassNames = PatchStateRegistry.getPatchClassNames();

        List<String> unknown = new ArrayList<>();
        for (String patchClassName : patchClassNames) {
            if (!knownPatchClassNames.contains(patchClassName)) {
                unknown.add(patchClassName);
            }
        }

        if (!unknown.isEmpty()) {
            for (String patchClassName : unknown) {
                Console.showMessage("No patch found with the name \"" + patchClassName + "\".");
            }
            return CommandResult.ERROR;
        }

        for (String patchClassName : patchClassNames) {
            boolean enabled = !PatchStateRegistry.isEnabled(patchClassName);
            PatchStateRegistry.setEnabled(patchClassName, enabled);
            Console.showMessage((enabled ? "Enabled" : "Disabled") + " the patch " + patchClassName + ".");
        }
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> getSuggestions(int parameter, List<String> previous, CommandContext context) {
        List<String> suggestions = PatchStateRegistry.getPatchClassNames();
        suggestions.removeAll(previous);
        return suggestions;
    }

}
