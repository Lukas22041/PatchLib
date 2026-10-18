package patchlib.console;

import org.lazywizard.console.BaseCommandWithSuggestion;
import org.lazywizard.console.Console;
import patchlib.agent.patch.PatchStateRegistry;

import java.util.ArrayList;
import java.util.List;

/** Console command to toggle the state of a patch */
public class TogglePatch implements BaseCommandWithSuggestion {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (args.isEmpty()) return CommandResult.BAD_SYNTAX;

        String patchClassName = args.trim();
        if (!PatchStateRegistry.getPatchClassNames().contains(patchClassName)) {
            Console.showMessage("No patch found with the name \"" + patchClassName + "\".");
            return CommandResult.ERROR;
        }

        boolean enabled = !PatchStateRegistry.isEnabled(patchClassName);
        PatchStateRegistry.setEnabled(patchClassName, enabled);
        Console.showMessage((enabled ? "Enabled" : "Disabled") + " the patch " + patchClassName + ".");
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> getSuggestions(int parameter, List<String> previous, CommandContext context) {
        if (parameter != 0) return new ArrayList<>();
        return PatchStateRegistry.getPatchClassNames();
    }

}
