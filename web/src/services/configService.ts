import { supabase } from '../supabaseClient';

export const configService = {
  async getDownloadUrl() {
    const { data, error } = await supabase
      .from('app_config')
      .select('value')
      .eq('key', 'download_url')
      .single();

    if (error) return null;
    return data.value as string;
  }
};
