import { supabase } from '../supabaseClient'

export const configService = {
  async getConfig() {
    const { data, error } = await supabase
      .from('app_config')
      .select('*')

    if (error) throw error

    const config: Record<string, string | number | boolean | null | Record<string, unknown>> = {}
    data.forEach(item => {
      config[item.key] = item.value
    })

    return config
  },

  async getDownloadUrl() {
    const config = await this.getConfig()
    return config.download_url as string || ''
  },

  async getDeliveryFee() {
    const config = await this.getConfig()
    return parseFloat(config.delivery_fee) || 0
  }
}
