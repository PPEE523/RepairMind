<template>
  <div class="equipment-cascader">
    <el-cascader
      v-model="selectedValue"
      :options="options"
      :props="cascaderProps"
      placeholder="请选择设备"
      clearable
      style="width: 100%"
      @change="handleChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'

interface EquipmentOption {
  value: string
  label: string
  children?: EquipmentOption[]
}

const props = defineProps<{
  modelValue?: string[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string[]): void
  (e: 'change', value: { deptId: string; equipType: string; equipModel: string }): void
}>()

const selectedValue = ref<string[]>(props.modelValue || [])
const options = ref<EquipmentOption[]>(getDefaultOptions())

const cascaderProps = {
  value: 'value',
  label: 'label',
  children: 'children'
}

// 监听外部值变化
watch(() => props.modelValue, (newVal) => {
  selectedValue.value = newVal || []
})

// 本地静态设备树数据
function getDefaultOptions(): EquipmentOption[] {
  return [
    {
      value: '1',
      label: '一号产线',
      children: [
        {
          value: 'engine',
          label: '发动机',
          children: [
            { value: 'ENGINE-V8-2024', label: 'ENGINE-V8-2024' },
            { value: 'ENGINE-V6-2023', label: 'ENGINE-V6-2023' }
          ]
        },
        {
          value: 'transmission',
          label: '变速箱',
          children: [
            { value: 'TRANS-6AT-2024', label: 'TRANS-6AT-2024' },
            { value: 'TRANS-8AT-2023', label: 'TRANS-8AT-2023' }
          ]
        }
      ]
    },
    {
      value: '2',
      label: '二号产线',
      children: [
        {
          value: 'hydraulic',
          label: '液压系统',
          children: [
            { value: 'HYD-SYS-2024', label: 'HYD-SYS-2024' }
          ]
        },
        {
          value: 'electrical',
          label: '电气系统',
          children: [
            { value: 'ELEC-SYS-2024', label: 'ELEC-SYS-2024' }
          ]
        }
      ]
    },
    {
      value: '3',
      label: '三号产线',
      children: [
        {
          value: 'cnc',
          label: '数控机床',
          children: [
            { value: 'CNC-850-2024', label: 'CNC-850-2024' },
            { value: 'CNC-1060-2023', label: 'CNC-1060-2023' }
          ]
        },
        {
          value: 'robot',
          label: '工业机器人',
          children: [
            { value: 'ROBOT-AR3-2024', label: 'ROBOT-AR3-2024' }
          ]
        }
      ]
    }
  ]
}

const handleChange = (value: string[]) => {
  emit('update:modelValue', value)

  if (value && value.length === 3) {
    emit('change', {
      deptId: value[0],
      equipType: value[1],
      equipModel: value[2]
    })
  }
}

onMounted(() => {
  // 使用本地静态数据，无需请求后端接口
})
</script>

<style scoped>
.equipment-cascader {
  width: 100%;
}
</style>