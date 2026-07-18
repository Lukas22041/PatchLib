package patchlib.agent.scan;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;
import patchlib.api.data.ClassData;

import java.util.List;

public record DiscoveryData(TypePool pool, ClassFileLocator locator, List<ClassData> classes) { }
