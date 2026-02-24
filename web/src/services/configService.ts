import { supabase } from '../supabaseClient'

export const configService = {
  async getConfig() {
    const { data, error } = await supabase
      .from('app_config')
      .select('*')

    if (error) throw error

    const config: Record<string, any> = {}
    data.forEach(item => {
      config[item.key] = item.value
    })

    return config
  },

  async getDownloadUrl() {
    const config = await this.getConfig()
    return config.download_url as string || ''
  }
}
