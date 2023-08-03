package shadows.modid;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.fml.common.Mod;

@Mod(ModClassRename.MODID)
public class ModClassRename {

    public static final String MODID = "modid";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public ModClassRename() {
        LOGGER.info("Hello World");
    }

}
