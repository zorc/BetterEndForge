package mod.beethoven92.betterendforge.common.rituals;

import java.awt.Point;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.Sets;

import mod.beethoven92.betterendforge.common.block.BlockProperties;
import mod.beethoven92.betterendforge.common.block.EndPortalBlock;
import mod.beethoven92.betterendforge.common.block.RunedFlavoliteBlock;
import mod.beethoven92.betterendforge.common.init.ModBlocks;
import mod.beethoven92.betterendforge.common.init.ModTags;
import mod.beethoven92.betterendforge.common.tileentity.EternalPedestalTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.state.BooleanProperty;
import net.minecraft.util.Direction;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.Features;
import net.minecraft.world.server.ServerWorld;

public class EternalRitual 
{
	private final static Set<Point> STRUCTURE_MAP = Sets.newHashSet(
			new Point(-4, -5), new Point(-4, 5), new Point(-6, 0),
			new Point(4, -5), new Point(4, 5), new Point(6, 0));
	private final static Set<Point> FRAME_MAP = Sets.newHashSet(
			new Point(0, 0), new Point(0, 6), new Point(1, 0),
			new Point(1, 6), new Point(2, 1), new Point(2, 5),
			new Point(3, 2), new Point(3, 3), new Point(3, 4));
	private final static Set<Point> PORTAL_MAP = Sets.newHashSet(
			new Point(0, 0), new Point(0, 1), new Point(0, 2),
			new Point(0, 3), new Point(0, 4), new Point(1, 0),
			new Point(1, 1), new Point(1, 2), new Point(1, 3),
			new Point(1, 4), new Point(2, 1), new Point(2, 2),
			new Point(2, 3));
	private final static Set<Point> BASE_MAP = Sets.newHashSet(
			new Point(3, 0), new Point(2, 0), new Point(2, 1), new Point(1, 1),
			new Point(1, 2), new Point(0, 1), new Point(0, 2));
	
	private final static Block BASE = ModBlocks.FLAVOLITE.tiles.get();
	private final static Block PEDESTAL = ModBlocks.ETERNAL_PEDESTAL.get();
	private final static Block FRAME = ModBlocks.FLAVOLITE_RUNED_ETERNAL.get();
	private final static Block PORTAL = ModBlocks.END_PORTAL_BLOCK.get();
	private final static BooleanProperty ACTIVE = BlockProperties.ACTIVATED;
	
	private World world;
	private Direction.Axis axis;
	private BlockPos center;
	private BlockPos exit;
	private boolean active = false;
	
	public EternalRitual(World world) 
	{
		this.world = world;
	}
	
	public EternalRitual(World world, BlockPos initial) 
	{
		this(world);
		this.configure(initial);
	}
	
	public boolean hasWorld() 
	{
		return this.world != null;
	}
	
	public void setWorld(World world) 
	{
		this.world = world;
	}
	
	private boolean isValid() 
	{
		return world != null && !world.isRemote() &&
			   center != null && axis != null &&
			   world.getDimensionKey()!= World.THE_NETHER;
	}
	
	public void checkStructure() 
	{
		if (!isValid()) return;
		Direction moveX, moveY;
		if (Direction.Axis.X == axis) 
		{
			moveX = Direction.EAST;
			moveY = Direction.NORTH;
		} 
		else 
		{
			moveX = Direction.SOUTH;
			moveY = Direction.EAST;
		}
		boolean valid = this.checkFrame();
		for (Point pos : STRUCTURE_MAP) 
		{
			BlockPos.Mutable checkPos = center.toMutable();
			checkPos.move(moveX, pos.x).move(moveY, pos.y);
			valid &= this.isActive(checkPos);
		}
		if (valid)
		{
			this.activatePortal();
		}
	}
	
	private boolean checkFrame() 
	{
		BlockPos framePos = center.down();
		Direction moveDir = Direction.Axis.X == axis ? Direction.NORTH: Direction.EAST;
		boolean valid = true;
		for (Point point : FRAME_MAP)
		{
			BlockPos pos = framePos.toMutable().move(moveDir, point.x).move(Direction.UP, point.y);
			BlockState state = world.getBlockState(pos);
			valid &= state.getBlock() instanceof RunedFlavoliteBlock;
			pos = framePos.toMutable().move(moveDir, -point.x).move(Direction.UP, point.y);
			state = world.getBlockState(pos);
			valid &= state.getBlock() instanceof RunedFlavoliteBlock;
		}
		return valid;
	}
	
	public boolean isActive() 
	{
		return this.active;
	}
	
	private void activatePortal() 
	{
		if (active) return;
		this.activatePortal(world, center);
		this.doEffects((ServerWorld) world, center);
		if (exit == null) 
		{
			this.exit = this.findPortalPos();
		} 
		else 
		{
			World targetWorld = this.getTargetWorld();
			this.activatePortal(targetWorld, exit);
		}
		this.active = true;
	}

	private void doEffects(ServerWorld serverWorld, BlockPos center) 
	{
		Direction moveX, moveY;
		if (Direction.Axis.X == axis) 
		{
			moveX = Direction.EAST;
			moveY = Direction.NORTH;
		} else {
			moveX = Direction.SOUTH;
			moveY = Direction.EAST;
		}
		for (Point pos : STRUCTURE_MAP) 
		{
			BlockPos.Mutable p = center.toMutable();
			p.move(moveX, pos.x).move(moveY, pos.y);
			serverWorld.spawnParticle(ParticleTypes.PORTAL, p.getX() + 0.5, p.getY() + 1.5, p.getZ() + 0.5, 20, 0, 0, 0, 1);
			serverWorld.spawnParticle(ParticleTypes.REVERSE_PORTAL, p.getX() + 0.5, p.getY() + 1.5, p.getZ() + 0.5, 20, 0, 0, 0, 0.3);
		}
		serverWorld.playSound(null, center, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.NEUTRAL, 16, 1);
	}
	
	private void activatePortal(World world, BlockPos center) 
	{
		BlockPos framePos = center.down();
		Direction moveDir = Direction.Axis.X == axis ? Direction.NORTH: Direction.EAST;
		BlockState frame = FRAME.getDefaultState().with(ACTIVE, true);
		FRAME_MAP.forEach(point -> {
			BlockPos pos = framePos.toMutable().move(moveDir, point.x).move(Direction.UP, point.y);
			BlockState state = world.getBlockState(pos);
			if (state.hasProperty(ACTIVE) && !state.get(ACTIVE)) 
			{
				world.setBlockState(pos, frame);
			}
			pos = framePos.toMutable().move(moveDir, -point.x).move(Direction.UP, point.y);
			state = world.getBlockState(pos);
			if (state.hasProperty(ACTIVE) && !state.get(ACTIVE)) {
				world.setBlockState(pos, frame);
			}
		});
		Direction.Axis portalAxis = Direction.Axis.X == axis ? Direction.Axis.Z : Direction.Axis.X;
		BlockState portal = PORTAL.getDefaultState().with(EndPortalBlock.AXIS, portalAxis);
		IParticleData effect = new BlockParticleData(ParticleTypes.BLOCK, portal);
		ServerWorld serverWorld = (ServerWorld) world;
		
		PORTAL_MAP.forEach(point -> {
			BlockPos pos = center.toMutable().move(moveDir, point.x).move(Direction.UP, point.y);
			if (!world.getBlockState(pos).isIn(PORTAL)) {
				world.setBlockState(pos, portal);
				serverWorld.spawnParticle(effect, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.5, 0.5, 0.5, 0.1);
				serverWorld.spawnParticle(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.5, 0.5, 0.5, 0.3);
			}
			pos = center.toMutable().move(moveDir, -point.x).move(Direction.UP, point.y);
			if (!world.getBlockState(pos).isIn(PORTAL)) 
			{
				world.setBlockState(pos, portal);
				serverWorld.spawnParticle(effect, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.5, 0.5, 0.5, 0.1);
				serverWorld.spawnParticle(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.5, 0.5, 0.5, 0.3);
			}
		});
	}
	
	public void removePortal() 
	{
		if (!active || !isValid()) return;
		World targetWorld = this.getTargetWorld();
		this.removePortal(world, center);
		this.removePortal(targetWorld, exit);
	}
	
	private void removePortal(World world, BlockPos center) 
	{
		BlockPos framePos = center.down();
		Direction moveDir = Direction.Axis.X == axis ? Direction.NORTH: Direction.EAST;
		FRAME_MAP.forEach(point -> {
			BlockPos pos = framePos.toMutable().move(moveDir, point.x).move(Direction.UP, point.y);
			BlockState state = world.getBlockState(pos);
			if (state.isIn(FRAME) && state.get(ACTIVE)) 
			{
				world.setBlockState(pos, state.with(ACTIVE, false));
			}
			pos = framePos.toMutable().move(moveDir, -point.x).move(Direction.UP, point.y);
			state = world.getBlockState(pos);
			if (state.isIn(FRAME) && state.get(ACTIVE)) 
			{
				world.setBlockState(pos, state.with(ACTIVE, false));
			}
		});
		PORTAL_MAP.forEach(point -> {
			BlockPos pos = center.toMutable().move(moveDir, point.x).move(Direction.UP, point.y);
			if (world.getBlockState(pos).isIn(PORTAL)) 
			{
				world.removeBlock(pos, false);
			}
			pos = center.toMutable().move(moveDir, -point.x).move(Direction.UP, point.y);
			if (world.getBlockState(pos).isIn(PORTAL)) 
			{
				world.removeBlock(pos, false);
			}
		});
		this.active = false;
	}
	
	private BlockPos findPortalPos() 
	{
		//MinecraftServer server = world.getServer();
		ServerWorld targetWorld = (ServerWorld) this.getTargetWorld();
		//Registry<DimensionType> registry = server.getRegistryManager().getDimensionTypes();
		//double mult = registry.get(DimensionType.THE_END_ID).getCoordinateScale();
		DimensionType type = targetWorld.getDimensionType();
		double mult = type.getCoordinateScale();
		
		BlockPos.Mutable basePos = center.toMutable().setPos(center.getX() / mult, center.getY(), center.getZ() / mult);
		Direction.Axis portalAxis = Direction.Axis.X == axis ? Direction.Axis.Z : Direction.Axis.X;
		if (checkIsAreaValid(targetWorld, basePos, portalAxis)) 
		{
			EternalRitual.generatePortal(targetWorld, basePos, portalAxis);
			if (portalAxis.equals(Direction.Axis.X)) 
			{
				return basePos.toImmutable();
			} 
			else 
			{
				return basePos.toImmutable();
			}
		} 
		else 
		{
			Direction direction = Direction.EAST;
			BlockPos.Mutable checkPos = basePos.toMutable();
			for (int step = 1; step < 64; step++) 
			{
				for (int i = 0; i < step; i++)
				{
					checkPos.setY(5);
					int ceil = targetWorld.getChunk(basePos).getTopBlockY(Heightmap.Type.WORLD_SURFACE, checkPos.getX(), checkPos.getZ()) + 1;
					if (ceil < 5) continue;
					while(checkPos.getY() < ceil) 
					{
						if(checkIsAreaValid(targetWorld, checkPos, portalAxis)) 
						{
							EternalRitual.generatePortal(targetWorld, checkPos, portalAxis);
							if (portalAxis.equals(Direction.Axis.X)) 
							{
								return checkPos.toImmutable();
							} 
							else 
							{
								return checkPos.toImmutable();
							}
						}
						checkPos.move(Direction.UP);
					}
					checkPos.move(direction);
				}
				direction = direction.rotateY();
			}
		}
		if (targetWorld.getDimensionKey() == World.THE_END) 
		{
			Features.END_ISLAND.generate(targetWorld, targetWorld.getChunkProvider().getChunkGenerator(), new Random(basePos.toLong()), basePos.down());
		} 
		else 
		{
			basePos.setY(targetWorld.getChunk(basePos).getTopBlockY(Heightmap.Type.WORLD_SURFACE, basePos.getX(), basePos.getZ()) + 1);
		}
		EternalRitual.generatePortal(targetWorld, basePos, portalAxis);
		if (portalAxis.equals(Direction.Axis.X)) 
		{
			return basePos.toImmutable();
		} 
		else 
		{
			return basePos.toImmutable();
		}
	}
	
	private World getTargetWorld() 
	{
		RegistryKey<World> target = world.getDimensionKey() == World.THE_END ? World.OVERWORLD : World.THE_END;
		return world.getServer().getWorld(target);
	}
	
	private boolean checkIsAreaValid(World world, BlockPos pos, Direction.Axis axis) 
	{
		if (!isBaseValid(world, pos, axis)) return false;
		return EternalRitual.checkArea(world, pos, axis);
	}
	
	private boolean isBaseValid(World world, BlockPos pos, Direction.Axis axis) 
	{
		boolean solid = true;
		if (axis.equals(Direction.Axis.X)) 
		{
			pos = pos.down().add(0, 0, -3);
			for (int i = 0; i < 7; i++) 
			{
				BlockPos checkPos = pos.add(0, 0, i);
				BlockState state = world.getBlockState(checkPos);
				solid &= this.validBlock(world, checkPos, state);
			}
		} 
		else 
		{
			pos = pos.down().add(-3, 0, 0);
			for (int i = 0; i < 7; i++) 
			{
				BlockPos checkPos = pos.add(i, 0, 0);
				BlockState state = world.getBlockState(checkPos);
				solid &= this.validBlock(world, checkPos, state);
			}
		}
		return solid;
	}
	
	private boolean validBlock(World world, BlockPos pos, BlockState state) 
	{
		BlockState surfaceBlock = world.getBiome(pos).getGenerationSettings().getSurfaceBuilderConfig().getTop();
		return state.isOpaqueCube(world, pos) &&
			   (ModTags.validGenBlock(state) ||
			   state.isIn(surfaceBlock.getBlock()) ||
			   state.isIn(Blocks.STONE) ||
			   state.isIn(Blocks.SAND) ||
			   state.isIn(Blocks.GRAVEL));
	}
	
	public static void generatePortal(World world, BlockPos center, Direction.Axis axis) 
	{
		BlockPos framePos = center.down();
		Direction moveDir = Direction.Axis.X == axis ? Direction.EAST: Direction.NORTH;
		BlockState frame = FRAME.getDefaultState().with(ACTIVE, true);
		FRAME_MAP.forEach(point -> {
			BlockPos pos = framePos.toMutable().move(moveDir, point.x).move(Direction.UP, point.y);
			world.setBlockState(pos, frame);
			pos = framePos.toMutable().move(moveDir, -point.x).move(Direction.UP, point.y);
			world.setBlockState(pos, frame);
		});
		BlockState portal = PORTAL.getDefaultState().with(EndPortalBlock.AXIS, axis);
		PORTAL_MAP.forEach(point -> {
			BlockPos pos = center.toMutable().move(moveDir, point.x).move(Direction.UP, point.y);
			world.setBlockState(pos, portal);
			pos = center.toMutable().move(moveDir, -point.x).move(Direction.UP, point.y);
			world.setBlockState(pos, portal);
		});
		generateBase(world, framePos, moveDir);
	}
	
	private static void generateBase(World world, BlockPos center, Direction moveX) 
	{
		BlockState base = BASE.getDefaultState();
		Direction moveY = moveX.rotateY();
		BASE_MAP.forEach(point -> {
			BlockPos pos = center.toMutable().move(moveX, point.x).move(moveY, point.y);
			world.setBlockState(pos, base);
			pos = center.toMutable().move(moveX, -point.x).move(moveY, point.y);
			world.setBlockState(pos, base);
			pos = center.toMutable().move(moveX, point.x).move(moveY, -point.y);
			world.setBlockState(pos, base);
			pos = center.toMutable().move(moveX, -point.x).move(moveY, -point.y);
			world.setBlockState(pos, base);
		});
	}
	
	public static boolean checkArea(World world, BlockPos center, Direction.Axis axis) 
	{
		Direction moveDir = Direction.Axis.X == axis ? Direction.NORTH: Direction.EAST;
		for (BlockPos checkPos : BlockPos.getAllInBoxMutable(center.offset(moveDir.rotateY()), center.offset(moveDir.rotateYCCW()))) 
		{
			for (Point point : PORTAL_MAP) 
			{
				BlockPos pos = checkPos.toMutable().move(moveDir, point.x).move(Direction.UP, point.y);
				if (!world.getBlockState(pos).isAir()) return false;
				pos = checkPos.toMutable().move(moveDir, -point.x).move(Direction.UP, point.y);
				if (!world.getBlockState(pos).isAir()) return false;
			}
		}
		return true;
	}
	
	public void configure(BlockPos initial) 
	{
		BlockPos checkPos = initial.east(12);
		if (this.hasPedestal(checkPos)) 
		{
			this.axis = Direction.Axis.X;
			this.center = initial.east(6);
			return;
		}
		checkPos = initial.west(12);
		if (this.hasPedestal(checkPos)) 
		{
			this.axis = Direction.Axis.X;
			this.center = initial.west(6);
			return;
		}
		checkPos = initial.south(12);
		if (this.hasPedestal(checkPos)) 
		{
			this.axis = Direction.Axis.Z;
			this.center = initial.south(6);
			return;
		}
		checkPos = initial.north(12);
		if (this.hasPedestal(checkPos)) 
		{
			this.axis = Direction.Axis.Z;
			this.center = initial.north(6);
			return;
		}
		checkPos = initial.north(10);
		if (this.hasPedestal(checkPos)) 
		{
			this.axis = Direction.Axis.X;
			checkPos = checkPos.east(8);
			if (this.hasPedestal(checkPos)) 
			{
				this.center = initial.north(5).east(4);
				return;
			} 
			else 
			{
				this.center = initial.north(5).west(4);
				return;
			}
		}
		checkPos = initial.south(10);
		if (this.hasPedestal(checkPos)) 
		{
			this.axis = Direction.Axis.X;
			checkPos = checkPos.east(8);
			if (this.hasPedestal(checkPos)) 
			{
				this.center = initial.south(5).east(4);
				return;
			} 
			else
			{
				this.center = initial.south(5).west(4);
				return;
			}
		}
		checkPos = initial.east(10);
		if (this.hasPedestal(checkPos)) 
		{
			this.axis = Direction.Axis.Z;
			checkPos = checkPos.south(8);
			if (this.hasPedestal(checkPos)) 
			{
				this.center = initial.east(5).south(4);
				return;
			} 
			else
			{
				this.center = initial.east(5).north(4);
				return;
			}
		}
		checkPos = initial.west(10);
		if (this.hasPedestal(checkPos)) 
		{
			this.axis = Direction.Axis.Z;
			checkPos = checkPos.south(8);
			if (this.hasPedestal(checkPos)) 
			{
				this.center = initial.west(5).south(4);
				return;
			} 
			else
			{
				this.center = initial.west(5).north(4);
				return;
			}
		}
	}
	
	private boolean hasPedestal(BlockPos pos) 
	{
		return world.getBlockState(pos).isIn(PEDESTAL);
	}
	
	private boolean isActive(BlockPos pos)
	{
		BlockState state = world.getBlockState(pos);
		if (state.isIn(PEDESTAL)) 
		{
			EternalPedestalTileEntity pedestal = (EternalPedestalTileEntity) world.getTileEntity(pos);
			if (!pedestal.hasRitual()) 
			{
				pedestal.linkRitual(this);
			}
			return state.get(ACTIVE);
		}
		return false;
	}
	
	public CompoundNBT write(CompoundNBT compound) 
	{
		compound.put("center", NBTUtil.writeBlockPos((center)));
		if (exit != null) {
			compound.put("exit", NBTUtil.writeBlockPos(exit));
		}
		compound.putString("axis", axis.getName2());
		compound.putBoolean("active", active);
		return compound;
	}
	
	public void read(CompoundNBT nbt) 
	{
		this.axis = Direction.Axis.byName(nbt.getString("axis"));
		this.center = NBTUtil.readBlockPos(nbt.getCompound("center"));
		this.active = nbt.getBoolean("active");
		if (nbt.contains("exit")) {
			this.exit = NBTUtil.readBlockPos(nbt.getCompound("exit"));
		}
	}
}
