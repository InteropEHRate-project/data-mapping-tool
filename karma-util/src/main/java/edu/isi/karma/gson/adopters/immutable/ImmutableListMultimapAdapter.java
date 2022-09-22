package edu.isi.karma.gson.adopters.immutable;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

/**
 * Serialize and deserialize {@link ListMultimap}.
 * <br/>
 * References:
 * <br/>
 * - https://gist.github.com/jingyuyao/3030571e9051142c3a7ec498435a0c3c
 *
 * @author Danish
 */
public class ImmutableListMultimapAdapter
		implements JsonSerializer<ImmutableListMultimap>, JsonDeserializer<ImmutableListMultimap> {

	private static final Type asMapReturnType = getAsMapMethod().getGenericReturnType();

	private static Type asMapType(Type multimapType) {
		return TypeToken.of(multimapType).resolveType(asMapReturnType).getType();
	}

	private static Method getAsMapMethod() {
		try {
			return ListMultimap.class.getDeclaredMethod("asMap");
		} catch (NoSuchMethodException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	public JsonElement serialize(
			ImmutableListMultimap src, Type typeOfSrc, JsonSerializationContext context) {
		return context.serialize(src.asMap(), asMapType(typeOfSrc));
	}

	@Override
	public ImmutableListMultimap deserialize(
			JsonElement json, Type typeOfT,
			JsonDeserializationContext context)
			throws JsonParseException {

		Map<Object, Collection<Object>> asMap = context.deserialize(json, asMapType(typeOfT));
		ImmutableListMultimap.Builder<Object, Object> builder = ImmutableListMultimap.builder();
		for (Map.Entry<Object, Collection<Object>> entry : asMap.entrySet()) {
			builder.putAll(entry.getKey(), entry.getValue());
		}
		return builder.build();
	}
}