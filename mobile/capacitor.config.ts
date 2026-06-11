import type { CapacitorConfig } from "@capacitor/cli";

const config: CapacitorConfig = {
  appId: "com.example.hegemony.mobile",
  appName: "Hegemony Assistant",
  webDir: "dist",
  server: {
    androidScheme: "https",
  },
};

export default config;
