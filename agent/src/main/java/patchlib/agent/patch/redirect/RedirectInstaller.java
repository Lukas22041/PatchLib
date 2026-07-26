package patchlib.agent.patch.redirect;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import patchlib.agent.patch.InstallationData;

import java.util.List;

public class RedirectInstaller {

    public static DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription,
                                                   MethodDescription.InDefinedShape methodDescription, List<InstallationData> installationDataList) {

        return builder;
    }

}
