/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package carpet.worldedit;

import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.entity.metadata.EntityProperties;
import com.sk89q.worldedit.extent.Extent;
import carpet.worldedit.internal.NBTConverter;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.NullWorld;
import com.sk89q.worldedit.world.entity.EntityTypes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;

import java.lang.ref.WeakReference;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

class CarpetWEEntity implements Entity {

    private final WeakReference<net.minecraft.entity.Entity> entityRef;

    CarpetWEEntity(net.minecraft.entity.Entity entity) {
        checkNotNull(entity);
        this.entityRef = new WeakReference<>(entity);
    }

    @Override
    public BaseEntity getState() {
        net.minecraft.entity.Entity entity = entityRef.get();
        if (entity != null) {
            ResourceLocation id = IRegistry.ENTITY_TYPE.getKey(entity.getType());
            NBTTagCompound tag = new NBTTagCompound();
            entity.writeWithoutTypeId(tag);
            return new BaseEntity(EntityTypes.get(id.toString()), NBTConverter.fromNative(tag));
        } else {
            return null;
        }
    }

    @Override
    public Location getLocation() {
        net.minecraft.entity.Entity entity = entityRef.get();
        if (entity != null) {
            Vector3 position = Vector3.at(entity.posX, entity.posY, entity.posZ);
            float yaw = entity.rotationYaw;
            float pitch = entity.rotationPitch;

            return new Location(CarpetWEAdapter.adapt(entity.world), position, yaw, pitch);
        } else {
            return new Location(NullWorld.getInstance());
        }
    }

    @Override
    public boolean setLocation(Location location) {
        // TODO unused atm
        return false;
    }

    @Override
    public Extent getExtent() {
        net.minecraft.entity.Entity entity = entityRef.get();
        if (entity != null) {
            return CarpetWEAdapter.adapt(entity.world);
        } else {
            return NullWorld.getInstance();
        }
    }

    @Override
    public boolean remove() {
        net.minecraft.entity.Entity entity = entityRef.get();
        if (entity != null) {
            entity.remove();
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getFacet(Class<? extends T> cls) {
        net.minecraft.entity.Entity entity = entityRef.get();
        if (entity != null) {
            if (EntityProperties.class.isAssignableFrom(cls)) {
                return (T) new CarpetWEEntityProperties(entity);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
