package fr.adrien1106.reframed.client;

import fr.adrien1106.reframed.client.model.apperance.CamoAppearanceManager;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.item.ItemConvertible;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class ReFramedModelProvider {
	private final Map<Identifier, UnbakedModel> models = new HashMap<>();
	private final Map<ModelIdentifier, Identifier> itemAssignments = new HashMap<>();

	private volatile CamoAppearanceManager appearanceManager;

	/// Model resolver for the new Fabric API

	public @Nullable UnbakedModel resolveModel(Object id) {
		// First check if we have a direct model for this ID
		if (id instanceof Identifier identifier) {
			UnbakedModel model = models.get(identifier);
			if (model != null) return model;
		}

		// Then check if this is a ModelIdentifier (item model) that needs redirection
		// In 1.21.1, ModelIdentifier is a record type (not extending Identifier)
		// The model loading system may pass ModelIdentifier objects for item models
		if (id instanceof ModelIdentifier modelId) {
			Identifier modelPath = itemAssignments.get(modelId);
			if (modelPath != null) {
				return models.get(modelPath);
			}
		}

		return null;
	}
	
	/// camo appearance manager cache
	
	public CamoAppearanceManager getCamoAppearanceManager(Function<SpriteIdentifier, Sprite> spriteLookup) {
		//This is kind of needlessly sketchy using the "volatile double checked locking" pattern.
		//I'd like all frame models to use the same CamoApperanceManager, despite the model
		//baking process happening concurrently on several threads, but I also don't want to
		//hold up the model baking process too long.
		
		//Volatile field read:
		CamoAppearanceManager read = appearanceManager;
		
		if(read == null) {
			//Acquire a lock:
			synchronized(this) {
				//There's a chance another thread just initialized the object and released the lock
				//while we were waiting for it, so we do another volatile field read (the "double check"):
				read = appearanceManager;
				if(read == null) {
					//If no-one has initialized it still, I guess it falls to us
					read = appearanceManager = new CamoAppearanceManager(spriteLookup);
				}
			}
		}
		
		return Objects.requireNonNull(read);
	}
	
	public void dumpCache() {
		CamoAppearanceManager.dumpCahe();
		appearanceManager = null; //volatile write
	}
	
	public void addReFramedModel(Identifier id, UnbakedModel unbaked) {
		models.put(id, unbaked);
	}
	
	public void assignItemModel(Identifier model_id, Identifier... itemIds) {
		for(Identifier itemId : itemIds) itemAssignments.put(new ModelIdentifier(itemId, "inventory"), model_id);
	}
	
	public void assignItemModel(Identifier model_id, ItemConvertible... itemConvs) {
		for(ItemConvertible itemConv : itemConvs) assignItemModel(model_id, Registries.ITEM.getId(itemConv.asItem()));
	}
}
