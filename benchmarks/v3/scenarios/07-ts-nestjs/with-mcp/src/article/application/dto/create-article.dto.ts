import { IsString, IsNotEmpty, MinLength, IsArray, IsOptional } from 'class-validator';

export class CreateArticleDto {
  @IsString()
  @IsNotEmpty()
  @MinLength(3)
  title!: string;

  @IsString()
  @IsNotEmpty()
  @MinLength(10)
  content!: string;

  @IsString()
  @IsNotEmpty()
  author!: string;

  @IsArray()
  @IsOptional()
  @IsString({ each: true })
  tags?: string[];
}
